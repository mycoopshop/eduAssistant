package ro.unitbv.eduassistant.service.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import io.fouad.jtb.core.JTelegramBot;
import io.fouad.jtb.core.beans.InlineKeyboardButton;
import io.fouad.jtb.core.builders.ApiBuilder;
import io.fouad.jtb.core.builders.ReplyMarkupBuilder;
import io.fouad.jtb.core.enums.ParseMode;
import io.fouad.jtb.core.exceptions.NegativeResponseException;
import ro.unitbv.eduassistant.api.exception.EduAssistantApiException;
import ro.unitbv.eduassistant.model.LessonSession;
import ro.unitbv.eduassistant.model.MultipleChoiceResponse;
import ro.unitbv.eduassistant.model.Question;
import ro.unitbv.eduassistant.model.Registration;
import ro.unitbv.eduassistant.model.Response;
import ro.unitbv.eduassistant.model.VariantValue;
import ro.unitbv.eduassistant.repo.LessonSessionRepo;
import ro.unitbv.eduassistant.repo.QuestionRepo;
import ro.unitbv.eduassistant.repo.RegistrationRepo;
import ro.unitbv.eduassistant.repo.ResponseDao;
import ro.unitbv.eduassistant.service.QuestionService;
import ro.unitbv.eduassistant.util.Defaults;

@Service
public class QuestionServiceImpl implements QuestionService {

	/** The Constant LOGGER. */
	public static final Logger LOGGER = LogManager.getLogger();

	private Gson gson = new Gson();

	@Autowired
	private JTelegramBot bot;

	@Autowired
	private LessonSessionRepo lessonSessionRepo;

	@Autowired
	private QuestionRepo questionRepo;

	@Autowired
	private ResponseDao responseRepo;

	@Autowired
	private RegistrationRepo registrationRepo;

	@Override
	public void sendQuestionToRegisteredStudents(String lessonSessionKey, long questionId) {

		LessonSession lessonSession = lessonSessionRepo.findBySessionKey(lessonSessionKey).orElseThrow(
				() -> new IllegalArgumentException(String.format("The sessionKey %s is invalid", lessonSessionKey)));
		if (lessonSession.getState().equals(Defaults.STATUS_CLOSE)) {
			throw new EduAssistantApiException("The session of the requested lesson is closed. Open a new session");
		}

		List<Long> registeredStudents = lessonSession.getRegistations().stream()
				.map(reg -> reg.getStudent().getChatbotId()).collect(Collectors.toList());

		Question question = lessonSession.getLesson().getQuestions().stream().filter(q -> q.getId().equals(questionId))
				.findFirst().orElseThrow(
						() -> new IllegalArgumentException(String.format("The questionId %s is invalid", questionId)));

		registeredStudents.forEach(chatId -> sendQuestion(
				String.format("<b>Question %d</b> <code>%s</code>", questionId, question.getQuestion()),
				generateRondomVariants(question.getMultipleChoiceQuestion().getVariants(), question.getId(),
						lessonSession.getId()),
				chatId));

		LOGGER.info("Finish sending the question to all students");

	}

	private InlineKeyboardButton[][] generateRondomVariants(List<VariantValue> variants, Long questId,
			Long sessionId) {
		InlineKeyboardButton[][] buttons = new InlineKeyboardButton[variants.size()][1];
		Collections.shuffle(variants);
		for (int id = 0; id < variants.size(); id++) {
			CallbackData callbackData = new CallbackData(variants.get(id).getId(), questId, sessionId);
			buttons[id][0] = new InlineKeyboardButton(String.format("%d. %s", id + 1, variants.get(id).getValue()),
					null, gson.toJson(callbackData), null);
		}
		return buttons;
	}

	private void sendQuestion(String question, InlineKeyboardButton[][] variants, long chatId) {
		try {
			ApiBuilder.api(bot).sendMessage(question).toChatId(chatId)
					.applyReplyMarkup(ReplyMarkupBuilder.attachInlineKeyboard(variants).toReplyMarkup())
					.parseMessageAs(ParseMode.HTML).execute();
			LOGGER.debug(String.format("Question was send to chatid %s", chatId));
		} catch (NegativeResponseException | IOException e) {
			LOGGER.error("Error occured when tryning to reply to a message ", e);
			throw new IllegalStateException("Error occured when tryning to reply to a message", e);
		}

	}

	@Override
	public String checkCorrectness(Long lessonSessionId, Long questId,  int selectedRspId, long chatId) {
		Question question = questionRepo.findById(questId).orElseThrow(
				() -> new IllegalArgumentException(String.format("The question id: %s is invalid", questId)));
		boolean isCorrect = question.getMultipleChoiceQuestion().getCorrectVriantId() == selectedRspId;

		Registration registration = registrationRepo.getRegistration(lessonSessionId, chatId)
				.orElseThrow(() -> new IllegalArgumentException(String.format("The student with chat id: %s id was not found", chatId)));

		Response response = new Response();
		response.setQuestion(question);
		response.setRegistration(registration); 
		response.setMultipeChoiceQuestion(new MultipleChoiceResponse(question.getMultipleChoiceQuestion().getVariants().stream().filter(v  ->v.getId() == selectedRspId).findAny().orElse(new VariantValue()).getValue(), isCorrect));
		responseRepo.save(response);

		String hint = null;
		if (!isCorrect) {
			hint = question.getMultipleChoiceQuestion().getVariants().stream()
					.filter(v -> v.getId() == selectedRspId).findFirst().orElse(new VariantValue())
					.getHint();
		}
		return hint;
	}
}
