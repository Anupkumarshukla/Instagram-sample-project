package com.android.anupkumar.instagram.data.repository

import com.android.anupkumar.instagram.data.local.db.DatabaseService
import com.android.anupkumar.instagram.data.model.Dummy
import com.android.anupkumar.instagram.data.remote.NetworkService
import com.android.anupkumar.instagram.data.remote.request.DummyRequest
import io.reactivex.Single
import javax.inject.Inject

class DummyRepository @Inject constructor(
    private val networkService: NetworkService,
    private val databaseService: DatabaseService
) {

    fun fetchDummy(id: String): Single<List<Dummy>> =
        networkService.doDummyCall(DummyRequest(id)).map { it.data }

}