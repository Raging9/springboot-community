package com.nowcoder.community.controller;

import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.Event;
import com.nowcoder.community.entity.Page;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.event.EventProducer;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant{

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

    @Autowired
    private EventProducer eventProducer;

    /**
     * 关注
     * @param entityType
     * @param entityId
     * @return
     */
    @LoginRequired
    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType,int entityId){
        User user = hostHolder.getUser();
        followService.follow(user.getId(),entityType,entityId);

        //触发关注事件
        Event event = new Event()
                .setTopic(TOPIC_FOLLOW)
                .setUserId(user.getId())
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId);
        eventProducer.fireEvent(event);
        return CommunityUtil.getJSONString(0,"已关注！");
    }

    /**
     * 取消关注
     * @param entityType
     * @param entityId
     * @return
     */
    @LoginRequired
    @PostMapping("/unFollow")
    @ResponseBody
    public String unFollow(int entityType,int entityId){
        User user = hostHolder.getUser();
        followService.unFollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJSONString(0,"已取消关注！");
    }

    /**
     * 某用户的关注列表
     * @param userId
     * @param page
     * @param model
     * @return
     */
    @GetMapping("/followees/{userId}")
    public String getFollowees(@PathVariable("userId")int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("用户不存在！");
        }
        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/followees/"+userId);
        page.setRows((int) followService.findFolloweeCount(userId, CommunityConstant.ENTITY_TYPE_User));

        List<Map<String,Object>> userList = followService.findFollowee(userId,page.getOffset(),page.getLimit());
        if (userList!=null){
            for (Map<String,Object> map :userList){
                User u = (User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);

        return "/site/followee";
    }


    /**
     * 某用户的粉丝列表
     * @param userId
     * @param page
     * @param model
     * @return
     */
    @GetMapping("/followers/{userId}")
    public String getFollowers(@PathVariable("userId")int userId, Page page, Model model){
        User user = userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("用户不存在！");
        }
        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/followers/"+userId);
        page.setRows((int) followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_User, userId));

        List<Map<String,Object>> userList = followService.findFollowers(userId,page.getOffset(),page.getLimit());
        if (userList!=null){
            for (Map<String,Object> map :userList){
                User u = (User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);

        return "/site/follower";
    }

    //判断登录用户是否关注了此实体（用户）
    private boolean hasFollowed(int userId){
        User user = hostHolder.getUser();
        if(user==null){
            return false;
        }
        return followService.hasFollowed(user.getId(),CommunityConstant.ENTITY_TYPE_User,userId);
    }
}
