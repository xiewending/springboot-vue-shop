<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'

import { getOrderDetail } from '../api/order'
import type { OrderDetail } from '../types/order'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<OrderDetail | null>(null)

async function loadDetail() {
  loading.value = true
  try {
    const { data } = await getOrderDetail(Number(route.params.id))
    order.value = data.data
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <section class="workspace wide">
    <header class="topbar">
      <div>
        <p class="eyebrow">订单详情</p>
        <h1>{{ order?.orderNo ?? '订单' }}</h1>
      </div>
      <div class="actions">
        <el-button :icon="ArrowLeft" @click="router.push('/orders')">返回</el-button>
        <el-button :icon="Refresh" :loading="loading" @click="loadDetail">刷新</el-button>
      </div>
    </header>

    <section v-if="order" v-loading="loading" class="detail-layout">
      <div class="detail-panel">
        <h2>订单信息</h2>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag>{{ order.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单金额">¥{{ Number(order.totalAmount).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ order.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-panel">
        <h2>客户信息</h2>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ order.customerName }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ order.customerPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ order.shippingAddress }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="detail-panel">
        <h2>商品明细</h2>
        <el-table :data="order.items" border>
          <el-table-column prop="productName" label="商品名称" min-width="180" />
          <el-table-column prop="unitPrice" label="单价" width="140">
            <template #default="{ row }">¥{{ Number(row.unitPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="90" />
          <el-table-column prop="totalPrice" label="小计" width="140">
            <template #default="{ row }">¥{{ Number(row.totalPrice).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </section>
</template>
