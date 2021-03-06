package jp.ne.icchannel.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import jp.ne.icchannel.backend.Constants.Companion.ES_INDEX_THREAD
import jp.ne.icchannel.backend.domain.Thread
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.FieldSortBuilder
import org.elasticsearch.search.sort.SortOrder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ThreadService(private var elasticsearchService: ElasticsearchService,
                    private val objectMapper: ObjectMapper) {

    @Value("\${data.limit}")
    var limit: Int = 0

    fun getNewThreadList(search: String?, category: String?, next: String?): List<Thread> {
        val searchSourceBuilder = SearchSourceBuilder()
        searchSourceBuilder.size(limit)

        val boolQueryBuilder = BoolQueryBuilder()
        if (next is String) { boolQueryBuilder.filter(QueryBuilders.rangeQuery("publishedDate").lt(next)) }
        if (!search.isNullOrEmpty()) { boolQueryBuilder.must(MatchQueryBuilder("title", search))}
        searchSourceBuilder.query(boolQueryBuilder)

        searchSourceBuilder.sort(FieldSortBuilder("publishedDate").order(SortOrder.DESC))

        val request = SearchRequest(ES_INDEX_THREAD).source(searchSourceBuilder)
        val response = elasticsearchService.search(request)
        val searchHit = response.hits.hits

        return searchHit.map{ hit ->
            val thread: Thread = objectMapper.convertValue(hit.sourceAsMap, Thread::class.java)
            thread.copy(id = hit.id)
        }.toList()
    }
}