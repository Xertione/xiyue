<template>
  <div>
    <van-nav-bar title="发布订单" left-arrow @click-left="router.back()" />
    <van-cell-group inset class="page-pad" style="margin: 12px">
      <van-field label="服务日期" :model-value="form.serviceDate" placeholder="选择日期" readonly is-link @click="showDate = true" />
      <van-field label="开始时间" :model-value="form.startHour !== null ? form.startHour + ':00' : ''" placeholder="选择时间" readonly is-link @click="showHour = true" />
      <van-field label="服务时长">
        <template #input>
          <van-stepper v-model="form.durationHours" :min="1" :max="12" />
        </template>
      </van-field>
      <van-field v-model="form.contactName" label="联系人" placeholder="请输入联系人姓名" />
      <van-field v-model="form.contactPhone" label="联系电话" placeholder="请输入手机号" type="tel" maxlength="11" />
      <van-field v-model="form.address" label="服务地址" placeholder="请输入详细地址" type="textarea" rows="2" autosize />
      <van-field v-model="form.amount" label="订单金额" placeholder="请输入金额" type="number">
        <template #button>元</template>
      </van-field>
    </van-cell-group>
    <div class="page-pad">
      <van-button type="primary" block round :loading="loading" @click="submit">确认发布</van-button>
    </div>

    <van-popup v-model:show="showDate" position="bottom" round>
      <van-date-picker v-model="datePick" :min-date="minDate" title="选择服务日期" @confirm="onDateConfirm" @cancel="showDate = false" />
    </van-popup>
    <van-popup v-model:show="showHour" position="bottom" round>
      <van-picker :columns="hourColumns" title="选择开始时间" @confirm="onHourConfirm" @cancel="showHour = false" />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { orderApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const showDate = ref(false)
const showHour = ref(false)
const minDate = new Date(Date.now() + 86400000)
const datePick = ref([])
const form = ref({
  serviceDate: '',
  startHour: null as number | null,
  durationHours: 1,
  contactName: '',
  contactPhone: '',
  address: '',
  amount: '' as string | number
})

const hourColumns = Array.from({ length: 24 }, (_, i) => ({ text: i + ':00', value: i }))

function onDateConfirm({ selectedValues }: any) {
  form.value.serviceDate = selectedValues.join('-')
  showDate.value = false
}
function onHourConfirm({ selectedValues }: any) {
  form.value.startHour = selectedValues[0]
  showHour.value = false
}

async function submit() {
  if (!form.value.serviceDate) return showToast('请选择服务日期')
  if (form.value.startHour === null) return showToast('请选择开始时间')
  if (form.value.startHour + form.value.durationHours > 24) return showToast('服务时间不能跨天')
  if (!form.value.contactName) return showToast('请输入联系人')
  if (!/^1[3-9]\d{9}$/.test(form.value.contactPhone)) return showToast('请输入正确手机号')
  if (!form.value.address) return showToast('请输入服务地址')
  if (!form.value.amount || Number(form.value.amount) <= 0) return showToast('请输入有效金额')
  loading.value = true
  try {
    await orderApi.create({
      serviceDate: form.value.serviceDate,
      startHour: form.value.startHour,
      durationHours: form.value.durationHours,
      contactName: form.value.contactName,
      contactPhone: form.value.contactPhone,
      address: form.value.address,
      amount: Number(form.value.amount)
    })
    showToast('订单发布成功')
    router.replace('/user/orders')
  } catch { /* 拦截器提示 */ } finally { loading.value = false }
}
</script>
