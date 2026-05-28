<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Search, View } from '@element-plus/icons-vue'

import { getOrders, getOrderStatusOptions, updateOrderStatus } from '../api/order'
import { useAuthStore } from '../stores/auth'
import type { Order, OrderStatusOption } from '../types/order'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const orders = ref<Order[]>([])
const total = ref(0)
const statusOptions = ref<OrderStatusOption[]>([])
const canViewDetail = computed(() => authStore.hasPermission('order:detail'))
const canChangeStatus = computed(() => authStore.hasPermission('order:status'))

const query = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined as number | undefined
})

const nextStatusMap: Record<number, number[]> = {
  0: [1, 4],
  1: [2, 4],
  2: [3],
  3: [],
  4: []
}

async function loadStatusOptions() {
  const { data } = await getOrderStatusOptions()
  statusOptions.value = data.data
}

async function loadOrders() {
  loading.value = true
  try {
    const { data } = await getOrders({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined,
      status: query.status
    })
    orders.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

function statusLabel(status: number) {
  return statusOptions.value.find((option) => option.value === status)?.label ?? '未知'
}

function statusTagType(status: number) {
  const types: Record<number, 'warning' | 'primary' | 'success' | 'info' | 'danger'> = {
    0: 'warning',
    1: 'primary',
    2: 'success',
    3: 'info',
    4: 'danger'
  }
  return types[status] ?? 'info'
}

function availableNextStatuses(order: Order) {
  const nextStatuses = nextStatusMap[order.status] ?? []
  return statusOptions.value.filter((option) => nextStatuses.includes(option.value))
}

function handleSearch() {
  query.page = 1
  loadOrders()
}

async function handleStatusChange(order: Order, nextStatus: number) {
  await ElMessageBox.confirm(
    `确认将订单 ${order.orderNo} 从“${order.statusText}”改为“${statusLabel(nextStatus)}”吗？`,
    '状态变更确认',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    }
  )
  await updateOrderStatus(order.id, nextStatus)
  ElMessage.success('订单状态已更新')
  await loadOrders()
}

onMounted(async () => {
  await loadStatusOptions()
  await loadOrders()
})
</script>

<template>
  <section class="workspace wide">
    <header class="topbar">
      <div>
        <p class="eyebrow">订单运营</p>
        <h1>订单管理</h1>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" :loading="loading" @click="loadOrders">刷新</el-button>
      </div>
    </header>

    <section class="toolbar">
      <el-input
        v-model="query.keyword"
        class="filter-input"
        clearable
        placeholder="搜索订单号、客户、手机号"
        :prefix-icon="Search"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="query.status" class="filter-select" clearable placeholder="订单状态">
        <el-option v-for="option in statusOptions" :key="option.value" :label="option.label" :value="option.value" />
      </el-select>
      <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
    </section>

    <el-table v-loading="loading" :data="orders" class="data-table" border>
      <el-table-column prop="orderNo" label="订单号" min-width="170" />
      <el-table-column prop="customerName" label="客户" width="140" />
      <el-table-column prop="customerPhone" label="手机号" width="140" />
      <el-table-column prop="totalAmount" label="订单金额" width="130">
        <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="150">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ row.statusText }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="190" />
      <el-table-column v-if="canViewDetail || canChangeStatus" label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button v-if="canViewDetail" :icon="View" link type="primary" @click="router.push(`/orders/${row.id}`)">
            详情
          </el-button>
          <el-dropdown v-if="canChangeStatus && availableNextStatuses(row).length > 0" trigger="click">
            <el-button link type="primary">变更状态</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="option in availableNextStatuses(row)"
                  :key="option.value"
                  @click="handleStatusChange(row, option.value)"
                >
                  {{ option.label }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        :total="total"
        @size-change="loadOrders"
        @current-change="loadOrders"
      />
    </div>
  </section>
</template>
