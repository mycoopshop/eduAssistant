package ro.unitbv.eduassistant.chatbot.handler;

import java.io.IOException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.fouad.jtb.core.TelegramBotApi;
import io.fouad.jtb.core.UpdateHandler;
import io.fouad.jtb.core.beans.CallbackQuery;
import io.fouad.jtb.core.beans.ChosenInlineResult;
import io.fouad.jtb.core.beans.InlineQuery;
import io.fouad.jtb.core.beans.InlineQueryResultArticle;
import io.fouad.jtb.core.beans.Message;
import io.fouad.jtb.core.builders.ApiBuilder;
import io.fouad.jtb.core.enums.ParseMode;
import io.fouad.jtb.core.exceptions.NegativeResponseException;
import ro.unitbv.eduassistant.service.RegistrationService;

@Service
public class GenericResponseHandler implements UpdateHandler {

	/** The Constant LOGGER. */
	public static final Logger LOGGER = LogManager.getLogger();
	
	@Autowired
	private RegistrationService registrationService;
	
	
	public void onCallbackQueryReceived(TelegramBotApi bot, int id, CallbackQuery callback) {
		LOGGER.info("Response callback "+callback.getData()+ "  callbackId "+callback.getId()+ " mesageId "+ callback.getMessage().getMessageId()+"  chatId "+callback.getFrom().getId());
		
		
		try {
			ApiBuilder.api(bot).sendMessage("Cool answer").toChatId(callback.getFrom().getId()).asReplyToMessage(callback.getMessage().getMessageId()).asSilentMessage().execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NegativeResponseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	public void onChosenInlineResultReceived(TelegramBotApi bot, int id, ChosenInlineResult inlineResult) {
	}

	public void onEditedMessageReceived(TelegramBotApi arg0, int arg1, Message arg2) {
	}

	public void onGetUpdatesFailure(Exception e) {
		LOGGER.error("Error occured on Update Handler ", e);
	}

	public void onInlineQueryReceived(TelegramBotApi bot, int id, InlineQuery inlineQuery) {
	}

	public void onMessageReceived(TelegramBotApi bot, int id, Message message) {
		String messageText = message.getText();
		String textWords[] = messageText.split("\\s+");
		LOGGER.info(String.format("The recived message from chatId: %s contains following words: %s", message.getChat().getId(), Arrays.toString(textWords)));
		
		String command = textWords[0].toUpperCase();
		switch (command) {
		case "REG":
			processCommandRegister(bot, message, textWords);
			break;
		default:
			sendResponse(bot, message.getChat().getId(), message.getMessageId(), "*Please provide a valid command. Call help to see what's avaialable*");
			break;
		}
	

	}
	
	private void processCommandRegister(TelegramBotApi telegramBotApi,  Message message, String textWords[]){
		if (textWords.length <= 1) {
			sendResponse(telegramBotApi, message.getChat().getId(), message.getMessageId(),
					"*Please provide a session key for registration*");
		} else {
			String sessionKey = textWords[1];
			String response = registrationService.registerNewStudentInSession(sessionKey,
					message.getChat().getId() + "");
			sendResponse(telegramBotApi, message.getChat().getId(), message.getMessageId(), response);
		}
	}
	
	private void sendResponse(TelegramBotApi bot, long chatId, int messageId, String response){
		try {
			ApiBuilder.api(bot).sendMessage(response).toChatId(chatId)
			.asReplyToMessage(messageId).asSilentMessage().parseMessageAs(ParseMode.MARKDOWN)
			.execute();
		} catch (NegativeResponseException | IOException e) {
			LOGGER.error("Error occured when tryning to reply to a message ", e);
			throw new IllegalStateException("Error occured when tryning to reply to a message",e);
		}

	}

}
