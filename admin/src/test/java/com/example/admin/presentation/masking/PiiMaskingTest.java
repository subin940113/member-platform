package com.example.admin.presentation.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiiMaskingTest {

    @Test
    void 이메일_로컬_부분이_2자_이상이면_첫_글자만_보이고_나머지는_마스킹된다() {
        assertThat(PiiMasking.maskEmail("user@example.com")).isEqualTo("u***@example.com");
    }

    @Test
    void 이메일_로컬_부분이_3자여도_첫_글자만_보이고_나머지는_마스킹된다() {
        assertThat(PiiMasking.maskEmail("abc@test.com")).isEqualTo("a***@test.com");
    }

    @Test
    void 이메일_로컬_부분이_2자이면_첫_글자만_보이고_나머지는_마스킹된다() {
        assertThat(PiiMasking.maskEmail("ab@test.com")).isEqualTo("a***@test.com");
    }

    @Test
    void 이메일_로컬_부분이_1자이면_별표와_도메인을_반환한다() {
        assertThat(PiiMasking.maskEmail("a@test.com")).isEqualTo("*@test.com");
    }

    @Test
    void 비어_있는_이메일은_마스킹_없이_비어_있는_값을_반환한다() {
        assertThat(PiiMasking.maskEmail(null)).isNull();
    }

    @Test
    void 공백_이메일은_마스킹_없이_공백을_반환한다() {
        assertThat(PiiMasking.maskEmail("")).isEqualTo("");
    }

    @Test
    void 휴대폰_번호_11자리는_앞_3자리와_뒤_4자리를_제외하고_마스킹된다() {
        assertThat(PiiMasking.maskPhone("01012345678")).isEqualTo("010****5678");
    }

    @Test
    void 휴대폰_번호가_8자_미만이면_마스킹_없이_그대로_반환한다() {
        assertThat(PiiMasking.maskPhone("0101234")).isEqualTo("0101234");
    }

    @Test
    void 비어_있는_휴대폰_번호는_마스킹_없이_비어_있는_값을_반환한다() {
        assertThat(PiiMasking.maskPhone(null)).isNull();
    }

    @Test
    void 공백_휴대폰_번호는_마스킹_없이_공백을_반환한다() {
        assertThat(PiiMasking.maskPhone("")).isEqualTo("");
    }

    @Test
    void 이름이_2자_이상이면_첫_글자만_보이고_나머지는_별표_두_개로_마스킹된다() {
        assertThat(PiiMasking.maskName("홍길")).isEqualTo("홍**");
    }

    @Test
    void 이름이_3자이면_첫_글자만_보이고_나머지는_별표_두_개로_마스킹된다() {
        assertThat(PiiMasking.maskName("홍길동")).isEqualTo("홍**");
    }

    @Test
    void 이름이_4자_이상이면_첫_글자만_보이고_나머지는_별표_두_개로_마스킹된다() {
        assertThat(PiiMasking.maskName("홍길동이")).isEqualTo("홍**");
    }

    @Test
    void 비어_있는_이름은_마스킹_없이_비어_있는_값을_반환한다() {
        assertThat(PiiMasking.maskName(null)).isNull();
    }

    @Test
    void 공백_이름은_마스킹_없이_공백을_반환한다() {
        assertThat(PiiMasking.maskName("")).isEqualTo("");
    }

    @Test
    void 한_글자_이름은_별표_하나로_마스킹된다() {
        assertThat(PiiMasking.maskName("홍")).isEqualTo("*");
    }
}
