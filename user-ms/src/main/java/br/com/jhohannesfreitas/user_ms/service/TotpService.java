package br.com.jhohannesfreitas.user_ms.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

    private final GoogleAuthenticator googleAuthenticator;

    public TotpService(GoogleAuthenticator googleAuthenticator) {
        this.googleAuthenticator = googleAuthenticator;
    }

//    public String gerarSecret() {
//        GoogleAuthenticatorKey googleAuthenticatorKey = googleAuthenticator.createCredentials();
//
//        return googleAuthenticatorKey.getKey();
//    }

    public GoogleAuthenticatorKey gerarCredenciais() {
        return googleAuthenticator.createCredentials();
    }

    public String gerarQrCodeUrl(String nomeAplicacao, String email, GoogleAuthenticatorKey credencials) {
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                nomeAplicacao,
                email,
                credencials
        );
    }

    public  boolean validarCodigo(String secret, String codigo) {

        try {
            int codigoNumerico = Integer.parseInt(codigo);

            return googleAuthenticator.authorize(
                    secret,
                    codigoNumerico);
        }  catch (NumberFormatException e) {
            return false;
        }
    }
}
