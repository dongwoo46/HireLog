package com.hirelog.api.company.application.view

data class CompanyDetailView(
    val id: Long = 0L,
    val name: String = "",
    val source: String = "",
    val isActive: Boolean = false,
    val brands: List<BrandView> = emptyList()
)


data class BrandView(
    val id: Long = 0L,
    val name: String = "",
    val verificationStatus: String = "",
    val isActive: Boolean = false
)
