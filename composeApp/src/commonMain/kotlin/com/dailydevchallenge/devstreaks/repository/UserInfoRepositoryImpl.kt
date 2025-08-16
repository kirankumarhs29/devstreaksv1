package com.dailydevchallenge.devstreaks.repository

import com.dailydevchallenge.devstreaks.sync.FirebaseUserHelper

class UserInfoRepositoryImpl(private val firebaseUserHelper: FirebaseUserHelper) :
    UserInfoRepository {
    override suspend fun getCurrentUserProfile(): PublicUserProfile? =
        firebaseUserHelper.getCurrentUserProfile()
}
