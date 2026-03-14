package com.hotel.scheduling_system;

import com.hotel.scheduling_system.view.MainView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import javafx.scene.Scene;

@SpringBootApplication
public class SchedulingSystemApplication extends Application {

	private ConfigurableApplicationContext springContext;

	@Override
	public void init() {
		// This starts Spring Boot in the background when JavaFX initializes
		springContext = new SpringApplicationBuilder(SchedulingSystemApplication.class).run();
	}

	@Override
	public void start(Stage primaryStage) {
		// Ask Spring Boot to give us the MainView (which has the AppController inside it)
		MainView mainView = springContext.getBean(MainView.class);

		Scene scene = new Scene(mainView);

		// Show the window
		primaryStage.setTitle("Hotel Scheduling System");
		primaryStage.setScene(scene);
		//primaryStage.setMaximized(true);

		primaryStage.show();
	}

	@Override
	public void stop() {
		// Ensures Spring Boot shuts down cleanly when you close the JavaFX window
		springContext.close();
		Platform.exit();
	}

	public static void main(String[] args) {
		// Launch the JavaFX application cycle
		Application.launch(SchedulingSystemApplication.class, args);
	}
}