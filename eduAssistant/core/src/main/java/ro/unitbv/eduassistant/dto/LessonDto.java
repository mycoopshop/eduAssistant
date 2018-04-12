package ro.unitbv.eduassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LessonDto {

	private Long id; 
	private String name;
	private String description;
	
}
