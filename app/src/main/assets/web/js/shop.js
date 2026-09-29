/**
 * 商城通用交互脚本，打通 Android 原生交互与纯网页动效
 */

// 原生触觉反馈震动
function vibrate(ms) {
    if (window.Android && typeof window.Android.vibrate === 'function') {
        window.Android.vibrate(ms || 50);
    }
}

// 优雅提示框（优先调用 Android 原生 Toast，原生不可用时降级为浮动动画 Toast）
function showToast(message) {
    vibrate(40);
    if (window.Android && typeof window.Android.showToast === 'function') {
        window.Android.showToast(message);
        return;
    }
    var oldToast = document.querySelector('.web_toast');
    if (oldToast) oldToast.remove();

    var toast = document.createElement('div');
    toast.className = 'web_toast';
    toast.innerText = message;
    document.body.appendChild(toast);
    setTimeout(function() {
        if (toast && toast.parentNode) toast.parentNode.removeChild(toast);
    }, 2100);
}

// 模拟领券
function claimCoupon(btn, amount) {
    if (btn.classList.contains('claimed')) {
        showToast("该优惠券您已领取过了");
        return;
    }
    btn.classList.add('claimed');
    btn.innerText = "已领取";
    btn.style.background = "#E0E0E0";
    btn.style.color = "#9E9E9E";
    btn.style.boxShadow = "none";
    showToast("🎉 恭喜获得 " + amount + " 元优惠券！下单自动立减");
}

// 模拟签到领金币
function doSignin(btn) {
    if (btn.classList.contains('signed')) {
        showToast("今日已打卡签到，明天再来领更多金币吧！");
        return;
    }
    btn.classList.add('signed');
    btn.innerText = "今日已签到";
    btn.style.background = "#4CAF50";
    btn.style.color = "#FFFFFF";
    
    // 更新打卡天数与金币
    var todayEl = document.querySelector('.signin_day.active');
    if (todayEl) {
        todayEl.classList.remove('active');
        todayEl.classList.add('done');
    }
    var goldEl = document.getElementById('my_gold_count');
    if (goldEl) {
        var cur = parseInt(goldEl.innerText) || 0;
        goldEl.innerText = cur + 50;
    }
    showToast("✨ 签到成功！淘金币 +50，当前可抵 0.50 元");
}

// 话费充值选择
var selectedRechargePrice = 98.5;
var selectedRechargeFace = 100;
function selectRecharge(el, face, price) {
    var items = document.querySelectorAll('.recharge_item');
    items.forEach(function(item) { item.classList.remove('selected'); });
    el.classList.add('selected');
    selectedRechargeFace = face;
    selectedRechargePrice = price;
    var priceEl = document.getElementById('pay_price_text');
    if (priceEl) priceEl.innerText = "立即支付 ￥" + price.toFixed(2);
}

// 模拟立即充值
function submitRecharge() {
    var phoneInput = document.getElementById('phone_input');
    var phone = phoneInput ? phoneInput.value.trim() : "";
    if (!phone || phone.length < 11) {
        showToast("请输入正确的11位手机号码");
        return;
    }
    showToast("正在为您充值 " + selectedRechargeFace + " 元话费...");
    setTimeout(function() {
        showToast("✅ 话费充值成功！预计 1-5 分钟内到账");
    }, 1200);
}

// 芭芭农场浇水
var farmWater = 100;
var farmProgress = 65;
function waterCrop(btn) {
    if (farmWater < 10) {
        showToast("水滴不足啦！做任务可获得更多水滴哦");
        return;
    }
    farmWater -= 10;
    farmProgress = Math.min(100, farmProgress + 5);
    
    var waterEl = document.getElementById('water_count');
    if (waterEl) waterEl.innerText = farmWater + "g";
    
    var progressEl = document.getElementById('crop_progress');
    var progressText = document.getElementById('crop_percent');
    if (progressEl) progressEl.style.width = farmProgress + "%";
    if (progressText) progressText.innerText = farmProgress + "%";

    var tree = document.getElementById('crop_tree');
    if (tree) {
        tree.style.transform = "scale(1.15)";
        setTimeout(function() { tree.style.transform = "scale(1.0)"; }, 300);
    }

    if (farmProgress >= 100) {
        showToast("🍎 果树成熟啦！已为您生成免费水果兑换券！");
    } else {
        showToast("💧 浇水成功！果树成长中 +5%");
    }
}

// 拍卖出价
function bidProduct(btn, increment) {
    var priceEl = document.getElementById('current_bid_price');
    if (priceEl) {
        var cur = parseInt(priceEl.innerText.replace(/[^0-9]/g, '')) || 100;
        cur += increment;
        priceEl.innerText = "￥" + cur;
        showToast("🔨 出价成功！当前领先价格 ￥" + cur);
    }
}
