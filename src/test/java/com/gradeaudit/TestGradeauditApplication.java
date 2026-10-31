package com.gradeaudit;

import org.springframework.boot.SpringApplication;

public class TestGradeauditApplication {

	public static void main(String[] args) {
		SpringApplication.from(GradeauditApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
