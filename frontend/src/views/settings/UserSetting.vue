<template>
    <div style="display: flex;max-width: 950px;margin: auto">
        <div class="settings-left">
            <card :icon="User" title="账号信息设置" desc="在这里编辑您的个人信息，您可以在隐私设置中选择是否展示这些信息"
                  v-loading="loading.form">
                <el-form :model="baseForm" :rules="rules" ref="baseFormRef" label-position="top"
                         style="margin: 0 10px 10px 10px">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="baseForm.username" maxlength="10"/>
                    </el-form-item>
                    <el-form-item label="性别">
                        <el-radio-group v-model="baseForm.gender">
                            <el-radio :label="0">男</el-radio>
                            <el-radio :label="1">女</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="手机号" prop="phone">
                        <el-input v-model="baseForm.phone" maxlength="11"/>
                    </el-form-item>
                    <el-form-item label="QQ号" prop="qq">
                        <el-input v-model="baseForm.qq" maxlength="13"/>
                    </el-form-item>
                    <el-form-item label="微信号" prop="wx">
                        <el-input v-model="baseForm.wx" maxlength="20"/>
                    </el-form-item>
                    <el-form-item label="个人简介" prop="desc">
                        <el-input v-model="baseForm.desc" type="textarea" :rows="6" maxlength="200"/>
                    </el-form-item>
                    <div>
                        <el-button :icon="Select" @click="saveDetails" :loading="loading.base"
                                   type="success">保存用户信息
                        </el-button>
                    </div>
                </el-form>
            </card>
            <card style="margin-top: 10px" :icon="Message" title="电子邮件设置"
                  desc="您可以在这里修改默认绑定的电子邮件地址">
                <el-form :rules="rules" @validate="onValidate" :model="emailForm" ref="emailFormRef"
                         label-position="top" style="margin: 0 10px 10px 10px">
                    <el-form-item label="电子邮件" prop="email">
                        <el-input v-model="emailForm.email"/>
                    </el-form-item>
                    <el-form-item prop="code">
                        <el-row style="width: 100%" :gutter="10">
                            <el-col :span="18">
                                <el-input placeholder="请获取验证码" v-model="emailForm.code"/>
                            </el-col>
                            <el-col :span="6">
                                <el-button type="success" style="width: 100%" :disabled="!isEmailValid || coldTime > 0"
                                           @click="sendEmailCode" plain>
                                    {{ coldTime > 0 ? `请稍后 ${coldTime} 秒` : '获取验证码' }}
                                </el-button>
                            </el-col>
                        </el-row>
                    </el-form-item>
                    <div>
                        <el-button :icon="Refresh" type="success" @click="modifyEmail">更新电子邮件</el-button>
                    </div>
                </el-form>
            </card>
        </div>
        <div class="settings-right">
            <div style="position: sticky;top: 20px">
                <card>
                    <div style="text-align: center;padding: 5px 15px 0 15px">
                        <el-avatar :size="70" :src="store.avatarUrl"
                                   :style="!store.avatarUrl ? { background: store.avatarColor(store.user.username) } : {}">
                            {{ store.avatarUrl ? '' : (store.user.username?.[0] || '') }}
                        </el-avatar>
                        <div style="margin: 5px 0">
                            <el-upload
                                    :action="axios.defaults.baseURL + '/api/image/avatar'"
                                    :show-file-list="false"
                                    :before-upload="beforeAvatarUpload"
                                    :on-success="uploadSuccess"
                                    :headers="accessHeader()">
                                <el-button size="small" round>修改头像</el-button>
                            </el-upload>
                        </div>
                        <div style="font-weight: bold">你好, {{ store.user.username }}</div>
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <div style="font-size: 14px;padding: 10px" class="text-secondary">
                        {{ desc || '这个用户很懒，没有填写个人简介~' }}
                    </div>
                </card>
                <card style="margin-top: 10px;font-size: 14px">
                    <div>账号注册时间: {{ registerTime }}</div>
                    <div class="text-secondary">欢迎加入 Campus Forum 校园论坛！</div>
                </card>
            </div>
        </div>
    </div>
</template>

<script setup>

import Card from "@/components/Card.vue"
import {Message, Refresh, Select, User} from "@element-plus/icons-vue"
import {useStore} from "@/stores/index"
import {computed, reactive, ref} from "vue"
import {accessHeader, get, post} from "@/net"
import {ElMessage} from "element-plus"
import axios from "axios"

/** Pinia 全局状态 */
const store = useStore()

/** 注册时间，格式化为本地时间字符串 */
const registerTime = computed(() => new Date(store.user.registerTime).toLocaleString())

/** 个人简介（右侧卡片也需显示，独立于 baseForm） */
const desc = ref('')

/** 账号信息表单组件引用，用于调用 validate() */
const baseFormRef = ref()
/** 邮箱修改表单组件引用 */
const emailFormRef = ref()

/** 账号基本信息表单数据 */
const baseForm = reactive({
    username: '',
    gender: 1,
    phone: '',
    qq: '',
    wx: '',
    desc: ''
})

/** 邮箱修改表单数据 */
const emailForm = reactive({
    email: '',
    code: ''
})

/**
 * 自定义用户名校验器，只允许中文、英文、数字
 *
 * @param rule    校验规则对象
 * @param value   当前输入值
 * @param callback 校验结果回调
 */
const validateUsername = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请输入用户名'))
    } else if (!/^[a-zA-Z0-9一-龥]+$/.test(value)) {
        callback(new Error('用户名不能包含特殊字符，只能是中文/英文'))
    } else {
        callback()
    }
}

/** 表单校验规则，key 对应 el-form-item 的 prop */
const rules = {
    username: [
        {validator: validateUsername, trigger: ['blur', 'change']},
        {min: 2, max: 10, message: '用户名的长度必须在2-10个字符之间', trigger: ['blur', 'change']},
    ], email: [
        {required: true, message: '请输入邮件地址', trigger: 'blur'},
        {type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change']}
    ]
}

/** 加载状态：form 控制页面骨架屏，base 控制保存按钮 loading */
const loading = reactive({
    form: true,
    base: false
})

/**
 * 保存用户信息，校验通过后提交到后端，同步更新全局状态
 */
function saveDetails() {
    baseFormRef.value.validate(isValid => {
        if (isValid) {
            loading.base = true
            post('/api/user/save-details', baseForm, () => {
                ElMessage.success('用户信息保存成功')
                store.user.username = baseForm.username
                desc.value = baseForm.desc
                loading.base = false
            }, (message) => {
                ElMessage.warning(message)
                loading.base = false
            })
        }
    })
}

/** 页面初始化：拉取用户详情并填入表单 */
get('/api/user/details', data => {
    baseForm.username = store.user.username
    baseForm.gender = data.gender
    baseForm.phone = data.phone
    baseForm.wx = data.wx
    baseForm.qq = data.qq
    baseForm.desc = desc.value = data.desc
    emailForm.email = store.user.email
    loading.form = false
})

/** 验证码发送冷却倒计时（秒），0 表示可以发送 */
const coldTime = ref(0)
/** 邮箱字段校验是否通过，控制"获取验证码"按钮的禁用状态 */
const isEmailValid = ref(true)

/**
 * 表单校验回调，追踪邮箱字段的实时校验状态
 *
 * @param prop     校验的字段名
 * @param isValid  校验是否通过
 */
const onValidate = (prop, isValid) => {
    if (prop === 'email')
        isEmailValid.value = isValid
}

/** 发送邮箱验证码，校验通过后请求后端，同时启动 60 秒倒计时 */
function sendEmailCode() {
    emailFormRef.value.validate(isValid => {
        if (isValid) {
            coldTime.value = 60
            get(`/api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
                ElMessage.success(`验证码已成功发送到邮箱：${emailForm.email}，请注意查收`)
                const handle = setInterval(() => {
                    coldTime.value--
                    if (coldTime.value === 0) {
                        clearInterval(handle)
                    }
                }, 1000)
            }, (message) => {
                ElMessage.warning(message)
                coldTime.value = 0
            })
        }
    })
}

/** 修改邮箱，校验通过后提交新邮箱和验证码到后端 */
function modifyEmail() {
    emailFormRef.value.validate(isValid => {
        if (isValid) {
            post('/api/user/modify-email', emailForm, () => {
                ElMessage.success('邮件修改成功')
                store.user.email = emailForm.email
                emailForm.code = ''
            })
        }
    })
}

/**
 * 头像上传前校验，仅允许 JPG/PNG 且不超过 2MB
 *
 * @param rawFile 待上传的原始文件
 * @return true 允许上传，false 拒绝
 */
function beforeAvatarUpload(rawFile) {
    if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
        ElMessage.error('头像只能是 JPG/PNG 格式的')
        return false
    } else if(rawFile.size / 1024 / 1024 > 2) {
        ElMessage.error('头像大小不能大于 2MB')
        return false
    }
    return true
}

/**
 * 头像上传成功回调，更新全局状态中的头像标识
 *
 * @param response 后端返回的响应，response.data 为新头像标识
 */
function uploadSuccess(response){
    ElMessage.success('头像上传成功')
    store.user.avatar = response.data
}
</script>

<style scoped>
.settings-left {
    flex: 1;
    margin: 20px;
}

.settings-right {
    width: 300px;
    margin: 20px 30px 20px 0;
}

:global(.dark) .text-secondary {
    color: #a0a3a8 !important;
}
</style>
