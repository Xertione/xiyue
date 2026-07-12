<template>
  <div class="page">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <van-tabbar route>
      <van-tabbar-item to="/aunt/grab-list" icon="gift-o">抢单大厅</van-tabbar-item>
      <van-tabbar-item to="/aunt/mine" icon="orders-o">我的订单</van-tabbar-item>
    </van-tabbar>
  </div>
</template>
