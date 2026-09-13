package br.com.jhohannesfreitas.room_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class RoomMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoomMsApplication.class, args);
	}

}
