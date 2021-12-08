package com.nowcoder.community;

import com.mysql.cj.protocol.a.NativeMessageBuilder;
import com.nowcoder.community.dao.DiscussPostMapper;
import com.nowcoder.community.dao.elasticsearch.DiscussPostRepository;
import com.nowcoder.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ElasticsearchTests {
    @Autowired
    private DiscussPostMapper postMapper;

    @Autowired
    private DiscussPostRepository postRepository;

    @Autowired
    private ElasticsearchTemplate template;

    @Test
    public void testInsert(){
        postRepository.save(postMapper.selectDiscussPostById(241));
        postRepository.save(postMapper.selectDiscussPostById(242));
        postRepository.save(postMapper.selectDiscussPostById(243 ));
    }

    @Test
    public void testInsertList(){
        postRepository.saveAll(postMapper.selectDiscussPosts(101,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(102,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(103,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(111,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(112,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(131,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(132,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(133,0,100));
        postRepository.saveAll(postMapper.selectDiscussPosts(134,0,100));
    }

    @Test
    public void testUpdate(){
        DiscussPost discussPost = postMapper.selectDiscussPostById(231);
        discussPost.setContent("冲");
        postRepository.save(discussPost);
    }

    @Test
    public void testDelete(){
        postRepository.deleteById(231);
    }

    @Test
    public void testSearchByRepository(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();


        Page<DiscussPost> search = postRepository.search(searchQuery);
        System.out.println(search.getTotalElements());
        System.out.println(search.getTotalPages());
        System.out.println(search.getNumber());
        System.out.println(search.getSize());
        for (DiscussPost post:search){
            System.out.println(post);
        }
    }

    @Test
    public void testSearchByTemplate(){
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬","title","content"))
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0,10))
                .withHighlightFields(
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();


        AggregatedPage<DiscussPost> discussPosts = template.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> aClass, Pageable pageable) {
                SearchHits hits = response.getHits();
                if(hits.getTotalHits()<=0){
                    return null;
                }
                List<DiscussPost> list = new ArrayList<>();
                for(SearchHit hit : hits){
                    DiscussPost post = new DiscussPost();
                    String id = hit.getSourceAsMap().get("id").toString();
                    post.setId(Integer.valueOf(id));

                    String userId = hit.getSourceAsMap().get("userId").toString();
                    post.setUserId(Integer.valueOf(userId));

                    String title = hit.getSourceAsMap().get("title").toString();
                    post.setTitle(title);

                    String content = hit.getSourceAsMap().get("content").toString();
                    post.setContent(content);

                    String status = hit.getSourceAsMap().get("status").toString();
                    post.setStatus(Integer.valueOf(status));

                    String createTime = hit.getSourceAsMap().get("createTime").toString();
                    post.setCreateTime(new Date(Long.valueOf(createTime)));

                    String commentCount = hit.getSourceAsMap().get("commentCount").toString();
                    post.setCommentCount(Integer.valueOf(commentCount));

                    //处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if(titleField!=null){
                        post.setTitle(titleField.getFragments()[0].toString());
                    }

                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if(contentField!=null){
                        post.setContent(contentField.getFragments()[0].toString());
                    }
                    list.add(post);

                }
                return new AggregatedPageImpl(list,pageable,
                        hits.getTotalHits(),response.getAggregations(),response.getScrollId(),
                        hits.getMaxScore());
            }

            @Override
            public <T> T mapSearchHit(SearchHit searchHit, Class<T> aClass) {
                return null;
            }
        });
        System.out.println(discussPosts.getTotalElements());
        System.out.println(discussPosts.getTotalPages());
        System.out.println(discussPosts.getNumber());
        System.out.println(discussPosts.getSize());
        for (DiscussPost post:discussPosts){
            System.out.println(post);
        }

    }

}
