package com.akash.newsapp.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Log
import com.akash.newsapp.categoryconstants.Category
import com.akash.newsapp.data.repositories.NewsRepository
import com.akash.newsapp.data.response.NewsArticle
import kotlinx.coroutines.*
import java.lang.Exception

class ArticleViewModel constructor(val newsRepository: NewsRepository) : ViewModel() {
    private val TAG = ArticleViewModel::class.java.simpleName
    private val generalArticleList = MutableLiveData<List<NewsArticle>>().apply {
        value = emptyList()
    }
    private val message = MutableLiveData<String>().apply {
        value = ""
    }
    val viewModelMessage: LiveData<String> = message
    val articleList: LiveData<List<NewsArticle>> = generalArticleList
    fun getArticlesByCategory(category: String, page : Int = 1) {
        Log.e(TAG, "getArticles invoked ")
        GlobalScope.launch {
            try {
                val response = newsRepository.getArticlesByCategoryAsync(category).await()
                withContext(Dispatchers.Main){
                    generalArticleList.value = response.articles
                }

            } catch (e: Exception) {
                Log.e(TAG,"exception : ${e.localizedMessage}")
                withContext(Dispatchers.Main){
                    message.value = e.localizedMessage
                }
            }
        }
    }

}