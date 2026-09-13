package br.com.jhohannesfreitas.server_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer
public class ServerMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ServerMsApplication.class, args);
	}

}
