$(function(){
	$(".follow-btn").click(follow);
});

function follow() {
	var btn = this;
	if($(btn).hasClass("btn-info")) {
		$.post(
			CONTEXT_PATH + "/follow",
			{
				"entityType":3,"entityId":$(btn).prev().val()
			},
			function (data){
				data = $.parseJSON(data);
				if(data.code == 0){
					window.location.reload();
				}else {
					alert(data.msg);
				}
			}
		);
		// 关注TA效果
		//$(btn).text("已关注").removeClass("btn-info").addClass("btn-secondary");
	} else {
		$.post(
			CONTEXT_PATH + "/unFollow",
			{
				"entityType":3,"entityId":$(btn).prev().val()
			},
			function (data){
				data = $.parseJSON(data);
				if(data.code == 0){
					window.location.reload();
				}else {
					alert(data.msg);
				}
			}
		);
		// 取消关注效果
		//$(btn).text("关注TA").removeClass("btn-secondary").addClass("btn-info");
	}
}