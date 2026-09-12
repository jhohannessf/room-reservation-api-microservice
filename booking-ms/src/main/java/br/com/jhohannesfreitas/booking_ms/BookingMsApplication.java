package br.com.jhohannesfreitas.booking_ms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;

@SpringBootApplication
@EnableFeignClients
public class BookingMsApplication {

	public static void main(String[] args) {
		SpringApplication.run(BookingMsApplication.class, args);
	}

}
