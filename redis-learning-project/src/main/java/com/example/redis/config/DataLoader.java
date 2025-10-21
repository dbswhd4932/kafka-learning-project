package com.example.redis.config;

import com.example.redis.domain.Product;
import com.example.redis.domain.User;
import com.example.redis.repository.ProductRepository;
import com.example.redis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 애플리케이션 시작 시 테스트 데이터 생성
 * - 상품 10,000개
 * - 사용자 100명
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final String[] CATEGORIES = {
        "전자기기", "의류", "식품", "도서", "스포츠용품",
        "가구", "완구", "화장품", "주방용품", "자동차용품"
    };

    private static final String[] PRODUCT_PREFIXES = {
        "프리미엄", "스탠다드", "베스트", "인기", "신상",
        "특가", "프로", "울트라", "슈퍼", "메가"
    };

    private static final String[] PRODUCT_TYPES = {
        "노트북", "마우스", "키보드", "모니터", "헤드셋",
        "셔츠", "바지", "신발", "가방", "모자",
        "책", "공책", "펜", "의자", "책상",
        "공", "라켓", "자전거", "운동화", "요가매트"
    };

    private final Random random = new Random();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("=== 데이터 로딩 시작 ===");

        // 기존 데이터가 있으면 건너뛰기
        long productCount = productRepository.count();
        if (productCount > 0) {
            log.info("이미 {}개의 상품이 존재합니다. 데이터 로딩을 건너뜁니다.", productCount);
            return;
        }

        // 상품 10,000개 생성
        log.info("10,000개의 상품 데이터 생성 중...");
        createProducts();

        // 사용자 100명 생성
        log.info("100명의 사용자 데이터 생성 중...");
        createUsers();

        log.info("=== 데이터 로딩 완료 ===");
        log.info("총 상품: {}개", productRepository.count());
        log.info("총 사용자: {}명", userRepository.count());
    }

    private void createProducts() {
        List<Product> products = new ArrayList<>();

        for (int i = 1; i <= 10000; i++) {
            String prefix = PRODUCT_PREFIXES[random.nextInt(PRODUCT_PREFIXES.length)];
            String type = PRODUCT_TYPES[random.nextInt(PRODUCT_TYPES.length)];
            String category = CATEGORIES[random.nextInt(CATEGORIES.length)];

            Product product = Product.builder()
                    .name(prefix + " " + type + " " + i)
                    .description(category + " 카테고리의 " + prefix + " " + type + "입니다.")
                    .price(generateRandomPrice())
                    .stockQuantity(random.nextInt(500) + 10)
                    .category(category)
                    .build();

            products.add(product);

            // 1000개씩 배치로 저장 (성능 최적화)
            if (i % 1000 == 0) {
                productRepository.saveAll(products);
                products.clear();
                log.info("{}개 상품 저장 완료...", i);
            }
        }

        // 남은 데이터 저장
        if (!products.isEmpty()) {
            productRepository.saveAll(products);
        }
    }

    private void createUsers() {
        List<User> users = new ArrayList<>();

        String[] names = {
            "김철수", "이영희", "박민수", "정수진", "최영수",
            "한지민", "오상훈", "윤서연", "임동혁", "강미래"
        };

        for (int i = 1; i <= 100; i++) {
            String name = names[random.nextInt(names.length)];

            User user = User.builder()
                    .username("user" + i)
                    .email("user" + i + "@example.com")
                    .fullName(name + i)
                    .build();

            users.add(user);
        }

        userRepository.saveAll(users);
    }

    private BigDecimal generateRandomPrice() {
        // 10,000원 ~ 5,000,000원 사이의 랜덤 가격
        int price = (random.nextInt(499) + 1) * 10000;
        return BigDecimal.valueOf(price);
    }

}
