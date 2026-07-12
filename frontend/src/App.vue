<template>
  <van-config-provider :theme-vars="themeVars">
    <router-view />
  </van-config-provider>
</template>

<script setup lang="ts">
// 全局 Vant 主题定制：teal 品牌色（清洁/信赖，全站一致性 lock，skill §4.2）
const themeVars = {
  primaryColor: '#0d9488',
  primaryColorLight: '#14b8a6',
  primaryColorDark: '#0f766e',
  buttonPrimaryBackgroundColor: '#0d9488',
  buttonPrimaryBorderColor: '#0d9488',
  tabbarItemActiveColor: '#0d9488',
  navBarIconColor: '#0d9488',
  navBarTextColor: '#0f172a',
  switchOnBackgroundColor: '#0d9488',
  stepperButtonRoundActiveColor: '#0d9488',
  tabsBottomBarColor: '#0d9488',
  checkboxCheckedIconColor: '#0d9488',
  radioCheckedIconColor: '#0d9488'
}
</script>
