package ro.unitbv.eduassistant.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "lesson_session")
public class LessonSession {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lesson_session_id_seq")
	@SequenceGenerator(name = "lesson_session_id_seq", sequenceName = "lesson_session_id_seq", allocationSize = 2)
	@Column(name = "id")
	private Long id;
	
	@Column(name = "session_key",unique=true)
	private String sessionKey;
	@Column(name = "state")
	private String state;
	
	@ManyToOne
	private Lesson lesson;
	
	@OneToMany(mappedBy="lessonSession")
	private List<Registration> registations;
}
