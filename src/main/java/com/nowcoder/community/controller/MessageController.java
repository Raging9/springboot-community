package com.nowcoder.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.nowcoder.community.entity.Message;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.MessageService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    @Autowired
    private MessageService messageService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    /**
     * 登录用户的会话列表
     * @param model
     * @param page
     * @return
     */
    @RequestMapping(path = "/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page) {
        User user = hostHolder.getUser();
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));

        // 会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        if (conversationList != null) {
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                int targetId = user.getId() == message.getFromId() ? message.getToId() : message.getFromId();
                map.put("target", userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations", conversations);

        // 查询未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);
        //查询未读通知的数量
        int noticeUnRead = messageService.findNoticeUnRead(user.getId(), null);
        model.addAttribute("noticeUnRead",noticeUnRead);

        return "/site/letter";
    }

    /**
     * 登录用户的某一个会话的详情列表
     * @param conversationId
     * @param page
     * @param model
     * @return
     */
    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model) {
        // 分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        // 私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message message : letterList) {
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);

        // 私信目标
        model.addAttribute("target", getLetterTarget(conversationId));
        //未读私信的id
        List<Integer> ids = getLetterIds(letterList);
        if (!ids.isEmpty()) {
            // 设置已读
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    //私信的目标
    private User getLetterTarget(String conversationId) {
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);

        if (hostHolder.getUser().getId() == id0) {
            return userService.findUserById(id1);
        } else {
            return userService.findUserById(id0);
        }
    }

    //获取未读的私信
    private List<Integer> getLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        if (letterList != null) {
            for (Message message : letterList) {
                if (hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    /**
     * 发送私信
     * @param toName
     * @param content
     * @return
     */
    @RequestMapping(path = "/letter/send", method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName, String content) {
        User target = userService.findUserByName(toName);
        if (target == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在!");
        }

        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if (message.getFromId() < message.getToId()) {
            message.setConversationId(message.getFromId() + "_" + message.getToId());
        } else {
            message.setConversationId(message.getToId() + "_" + message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    /**
     * 系统通知页面
     * @param model
     * @return
     */
    @GetMapping("/notice/list")
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();

        //查询评论类的最新的一条通知
        Message message = messageService.findLatestNotice(user.getId(),TOPIC_COMMENT);
        Map<String,Object> messageVo = new HashMap<>();
        messageVo.put("message",message);
        if(message!=null){
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String,Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVo.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("postId",data.get("postId"));
            //评论类通知总数
            int count = messageService.findNoticeCount(user.getId(),TOPIC_COMMENT);
            messageVo.put("count",count);
            //评论类通知未读数量
            int noticeUnRead = messageService.findNoticeUnRead(user.getId(), TOPIC_COMMENT);
            messageVo.put("unread",noticeUnRead);

        }
        model.addAttribute("commentNotice",messageVo);

        //查询点赞类的通知
        message= messageService.findLatestNotice(user.getId(),TOPIC_LIKE);
        messageVo = new HashMap<>();
        messageVo.put("message",message);
        if(message!=null){
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));
            messageVo.put("postId",data.get("postId"));

            int count = messageService.findNoticeCount(user.getId(),TOPIC_LIKE);
            messageVo.put("count",count);
            //未读数量
            int noticeUnRead = messageService.findNoticeUnRead(user.getId(), TOPIC_LIKE);
            messageVo.put("unread",noticeUnRead);

        }
        model.addAttribute("likeNotice",messageVo);

        //查询关注类的通知
        message= messageService.findLatestNotice(user.getId(),TOPIC_FOLLOW);
        messageVo = new HashMap<>();
        messageVo.put("message",message);
        if(message!=null){
            String content = HtmlUtils.htmlUnescape(message.getContent());
            HashMap<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            messageVo.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType",data.get("entityType"));
            messageVo.put("entityId",data.get("entityId"));


            int count = messageService.findNoticeCount(user.getId(),TOPIC_FOLLOW);
            messageVo.put("count",count);
            //未读数量
            int noticeUnRead = messageService.findNoticeUnRead(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unread",noticeUnRead);
        }
        model.addAttribute("followNotice",messageVo);


        //查询所有未读私信的数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询所有未读通知的数量
        int noticeUnRead = messageService.findNoticeUnRead(user.getId(), null);
        model.addAttribute("noticeUnRead",noticeUnRead);

        return "/site/notice";
    }

    @GetMapping("/notice/detail/{topic}")
    public String getNoticesDetail(@PathVariable("topic")String topic,Model model,Page page){
        User user = hostHolder.getUser();

        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));

        List<Message> noticeList = messageService.findNotices(user.getId(),topic,page.getOffset(),page.getLimit());

        List<Map<String,Object>> noticeVoList = new ArrayList<>();
        if(noticeList!=null){
            for (Message notice:noticeList){
                Map<String,Object> map = new HashMap<>();
                //通知
                map.put("notice",notice);
                //内容
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object> data = JSONObject.parseObject(content,HashMap.class);
                map.put("user",userService.findUserById((Integer) data.get("userId")));
                map.put("entityId",data.get("entityId"));
                map.put("entityType",data.get("entityType"));
                map.put("postId",data.get("postId"));
                //通知的作者(系统)
                map.put("fromUser",userService.findUserById(notice.getFromId()));

                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices",noticeVoList);

        //设置已读
        List<Integer> ids = getLetterIds(noticeList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/notice-detail";
    }
}
