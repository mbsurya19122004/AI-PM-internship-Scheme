package com.internshipplatform.config;

import com.internshipplatform.entity.Internship;
import com.internshipplatform.repository.InternshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final InternshipRepository internshipRepository;

    @Override
    public void run(String... args) {
        if (internshipRepository.count() > 0) {
            log.info("Database already seeded, skipping DataLoader");
            return;
        }

        List<Internship> internships = List.of(
            Internship.builder()
                .title("Software Engineering Intern")
                .company("Google")
                .description("Work on large-scale distributed systems and contribute to products used by billions. You will be paired with a mentor and have the opportunity to work on real projects.")
                .applicationLink("https://careers.google.com/jobs/results/123456/")
                .build(),
            Internship.builder()
                .title("Frontend Developer Intern")
                .company("Microsoft")
                .description("Build beautiful and performant web applications using React and TypeScript. Join the Azure DevOps team and help shape the future of developer tools.")
                .applicationLink("https://careers.microsoft.com/global/en/apply/789012/")
                .build(),
            Internship.builder()
                .title("Data Science Intern")
                .company("Amazon")
                .description("Analyze large datasets and build machine learning models to drive business decisions. Work with AWS services and cutting-edge ML frameworks.")
                .applicationLink("https://www.amazon.jobs/en/positions/345678/")
                .build(),
            Internship.builder()
                .title("Backend Developer Intern")
                .company("Meta")
                .description("Design and implement RESTful APIs using Java and Python. Contribute to the infrastructure that powers Facebook, Instagram, and WhatsApp.")
                .applicationLink("https://www.metacareers.com/jobs/901234/")
                .build(),
            Internship.builder()
                .title("Cloud Infrastructure Intern")
                .company("AWS")
                .description("Help build and maintain cloud infrastructure at scale. Work with EC2, S3, Lambda, and other AWS services in a fast-paced environment.")
                .applicationLink("https://aws.amazon.com/careers/567890/")
                .build(),
            Internship.builder()
                .title("Mobile App Developer Intern")
                .company("Flipkart")
                .description("Develop Android and iOS applications using Kotlin and Swift. Help millions of users shop online with a seamless mobile experience.")
                .applicationLink("https://www.flipkartcareers.com/internships/112233/")
                .build(),
            Internship.builder()
                .title("Cybersecurity Intern")
                .company("Deloitte")
                .description("Assess security vulnerabilities, perform penetration testing, and help clients secure their digital infrastructure.")
                .applicationLink("https://apply.deloitte.com/careers/445566/")
                .build(),
            Internship.builder()
                .title("AI/ML Research Intern")
                .company("NVIDIA")
                .description("Work on cutting-edge deep learning research and optimize models for GPU acceleration. Publish research papers and collaborate with world-class researchers.")
                .applicationLink("https://nvidia.com/careers/778899/")
                .build()
        );

        internshipRepository.saveAll(internships);
        log.info("Seeded {} sample internships", internships.size());
    }
}
