<template>
  <div class="page">
    <router-view v-slot="{ Component }">
      <keep-alive>
        <component :is="Component" />
      </keep-alive>
    </router-view>
    <van-tabbar route>
      <van-tabbar-item to="/user/aunts" icon="search">找阿姨</van-tabbar-item>
      <van-tabbar-item to="/user/orders" icon="orders-o">我的订单</van-tabbar-item>
    </van-tabbar>
  </div>
</template>
