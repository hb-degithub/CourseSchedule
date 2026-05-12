package com.hbde.courseschedule.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "theme_configs")
data class ThemeConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val primaryColor: Int,
    val backgroundImage: String? = null,
    val opacity: Float,
    val cornerRadius: Int,
    val fontSize: Int
)
