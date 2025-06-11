package com.ally.learn.springailearning;

import java.net.InetAddress;
import java.net.UnknownHostException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
@Slf4j
public class SpringaiLearningApplication {

	public static void main(String[] args) throws UnknownHostException {

		ConfigurableEnvironment env = SpringApplication.run(SpringaiLearningApplication.class, args).getEnvironment();
		String protocol = "http";
		String context = env.getProperty("server.servlet.context-path");
		context = context == null ? "" : context;
		System.out.println("-------------OK---------------");
		log.info("\n----------------------------------------------------------\n\t" +
				"Application '{}' is running! Access URLs:\n\t" +
				"Local: \t\t{}://localhost:{}\n\t" +
				"External: \t{}://{}:{}\n\t" +
				"swagger: \t{}://{}:{}/doc.html\n\t" +
				"ApiDocs: \t{}\n\t" +
				"active: \t{}\n\t" +
				"ContextPath: \t{}\n" +
				"----------------------------------------------------------",
			env.getProperty("spring.application.name"),
			protocol,
			env.getProperty("server.port") + context,
			protocol,
			InetAddress.getLocalHost().getHostAddress(),
			env.getProperty("server.port") + context,
			protocol,
			InetAddress.getLocalHost().getHostAddress(),
			env.getProperty("server.port") + context,
			env.getProperty("application.doc"),
			env.getProperty("spring.profiles.active"),
			context);


	}

}
