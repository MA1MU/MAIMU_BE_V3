package com.example.chosim.chosim.common.email;

import com.example.chosim.chosim.domain.auth.entity.Member;
import com.example.chosim.chosim.domain.maimu.entity.Maimu;
import com.example.chosim.chosim.domain.maimu.repository.MaimuRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final MaimuRepository maimuRepository;

    @Value("${spring.mail.username}")
    private String serviceName;

    @Qualifier("emailTaskExecutor")
    private final ThreadPoolTaskExecutor taskExecutor;

    private static final int MAX_RETRIES = 3;

    public void sendSummaryEmails() {
        List<Maimu> unNotifiedMaimus = maimuRepository.findAllIsNotifiedMaimusWithMember();
        if (unNotifiedMaimus.isEmpty()) {
            log.info("발송할 새로운 마이무 소식이 없습니다.");
            return;
        }

        Map<Member, List<Maimu>> memberMap = unNotifiedMaimus.stream()
                .collect(Collectors.groupingBy(m -> m.getGroup().getMember()));

        memberMap.forEach((member, maimus) -> {
            CompletableFuture.runAsync(() -> {
                boolean isSuccess = sendEmailWithRetry(member, maimus);
                if (isSuccess) {
                    updateMaimuStatus(maimus); // 발송 성공 시에만 DB 상태 업데이트
                }
            }, taskExecutor).exceptionally(ex -> {
                log.error("{}님 비동기 메일 처리 중 치명적 오류: {}", member.getEmail(), ex.getMessage());
                return null;
            });
        });
    }

    private boolean sendEmailWithRetry(Member member, List<Maimu> maimus) {
        String title = "[MAIMU] " + member.getMaimuProfile() + "님, 사물함에 새로운 소식이 도착했습니다!";
        String htmlContent = buildHtmlContent(member, maimus);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                sendMimeMessage(member.getEmail(), title, htmlContent);
                log.info("메일 발송 성공: {} (시도: {}회)", member.getEmail(), attempt);
                return true;
            } catch (Exception e) {
                log.warn("메일 발송 실패: {} (시도: {}회/{}회), 사유: {}", member.getEmail(), attempt, MAX_RETRIES, e.getMessage());
                if (attempt == MAX_RETRIES) {
                    log.error("{}님 최종 메일 발송 실패", member.getEmail());
                }
            }
        }
        return false;
    }

    private void sendMimeMessage(String to, String subject, String content) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setFrom(serviceName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(content, true);

        javaMailSender.send(mimeMessage);
    }

    @Transactional
    protected void updateMaimuStatus(List<Maimu> maimus) {
        List<Long> ids = maimus.stream().map(Maimu::getId).collect(Collectors.toList());
        maimuRepository.updateNotifiedStatus(ids);
        log.info("{}건의 마이무 알림 상태 업데이트 완료", ids.size());
    }

    private String buildHtmlContent(Member member, List<Maimu> maimus) {
        Map<String, Long> groupCounts = maimus.stream()
                .collect(Collectors.groupingBy(m -> m.getGroup().getGroupName(), Collectors.counting()));

        StringBuilder groupRows = new StringBuilder();
        groupCounts.forEach((groupName, count) ->
                groupRows.append("<li><strong>").append(groupName).append("</strong>: ")
                        .append(count).append("개의 읽지 않은 마이무</li>")
        );

        return String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: 'Arial', sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                    <h2 style="color: #ffcc00;">😁 안녕하세요, %s님!</h2>
                    <p>지금 <b>MAIMU</b> 사물함에 따끈따끈한 마음이 도착해있어요.</p>
                    <hr style="border: 0; border-top: 1px solid #eee;">
                    <ul style="list-style: none; padding: 0;">
                        %s
                    </ul>
                    <div style="margin-top: 30px; text-align: center;">
                        <a href="https://maimu.vercel.app" 
                           style="background-color: #ffcc00; color: white; padding: 12px 25px; text-decoration: none; border-radius: 5px; font-weight: bold;">
                           마이무 확인하러 가기
                        </a>
                    </div>
                </div>
            </body>
            </html>
            """, member.getMaimuProfile(), groupRows.toString());
    }

}
