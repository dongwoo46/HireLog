package com.hirelog.api.company.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@DisplayName("CompanyRelation 도메인 테스트")
class CompanyRelationTest {

    @Nested
    @DisplayName("CompanyRelation 생성 테스트")
    inner class CreateTest {

        @Test
        @DisplayName("create: 부모/자식 회사 ID와 관계 유형으로 생성되어야 한다")
        fun create_success() {
            // given
            val parentId = 1L
            val childId = 2L
            val type = CompanyRelationType.SUBSIDIARY

            // when
            val relation = CompanyRelation.create(parentId, childId, type)

            // then
            assertEquals(parentId, relation.parentCompanyId)
            assertEquals(childId, relation.childCompanyId)
            assertEquals(type, relation.relationType)
        }
    }
    
    // Note: private method validateInvariant test via reflection or just trust constructor logic if called.
    // In this codebase, validateInvariant is marked @PostPersist, @PostUpdate.
    // Testing purely domain logic might not trigger JPA listeners directly without integration test,
    // but we can check if there's any public validation logic.
    // Looking at source, validateInvariant is private and called by JPA callbacks.
    // We cannot easily test validInvariant without reflection or integration tests.
    // However, we can simulate what happens if we could invoke it.
    
    @Test
    @DisplayName("자기 자신과의 관계는 생성 후 검증 시 실패해야 한다 (Reflection Test)")
    fun validate_invariant() {
        // given
        val relation = CompanyRelation.create(1L, 1L, CompanyRelationType.SUBSIDIARY)
        
        // when & then
        val validationMethod: Method = CompanyRelation::class.java.getDeclaredMethod("validateInvariant")
        validationMethod.isAccessible = true
        
        val exception = assertThrows<InvocationTargetException> {
            validationMethod.invoke(relation)
        }
        
        assertEquals("Company cannot have relation with itself", exception.targetException.message)
    }
}
