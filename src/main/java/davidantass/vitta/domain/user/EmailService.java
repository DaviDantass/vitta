package davidantass.vitta.domain.user;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(User user, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setTo(user.getEmail());
            helper.setSubject("Vitta password reset");
            helper.setText("Hello " + user.getName() + ",\n\nUse this link to reset your password:\n"
                    + resetUrl + "\n\nThis link expires in one hour.", false);
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new IllegalStateException("Unable to send password reset email.", exception);
        }
    }
}
