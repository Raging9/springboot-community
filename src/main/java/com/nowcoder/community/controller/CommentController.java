package com.nowcoder.community.controller;

import com.nowcoder.community.entity.Comment;
import com.nowcoder.community.entity.DiscussPost;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.CommentService;
import com.nowcoder.community.service.DiscussPostService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;

    /**
     * 发布评论
     *
     * @param discussPostId
     * @param comment
     * @return
     */
    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment) {
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            if (target.getUserId() != hostHolder.getUser().getId()) {
                //触发评论事件（除了自己评论自己的帖子和评论）
                Event event = new Event();
                event.setTopic(TOPIC_COMMENT)
                        .setUserId(hostHolder.getUser().getId())
                        .setEntityType(comment.getEntityType())
                        .setEntityId(comment.getEntityId())
                        .setData("postId", discussPostId)
                        .setEntityUserId(target.getUserId());//帖子作者
                eventProducer.fireEvent(event);
            }
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            Comment target = commentService.findCommentById(comment.getEntityId());
            if (target.getUserId() != hostHolder.getUser().getId()) {
                //触发评论事件（除了自己评论自己的帖子和评论）
                Event event = new Event();
                event.setTopic(TOPIC_COMMENT)
                        .setUserId(hostHolder.getUser().getId())
                        .setEntityType(comment.getEntityType())
                        .setEntityId(comment.getEntityId())
                        .setData("postId", discussPostId)
                        .setEntityUserId(target.getUserId());//评论或者评论的评论（回复）的作者
                eventProducer.fireEvent(event);
            }
        }

        //如果评论给帖子
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            //触发发帖事件，把帖子存进es服务器(就是更新帖子的评论量)
            Event event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
        }
        return "redirect:/discuss/detail/" + discussPostId;
    }

}
