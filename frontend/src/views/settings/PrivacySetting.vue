<template>
    <div style="margin: auto;max-width: 600px">
        <div style="margin-top: 20px">
            <card :icon="Setting" title="隐私设置" desc="在这里设置哪些内容可以被其他人看到，请各位小伙伴注重自己的隐私" v-loading="saving">
                <div class="checkbox-list">
                    <el-checkbox @change="savePrivacy('phone', privacy.phone)"
                                 v-model="privacy.phone">公开展示我的手机号</el-checkbox>
                    <el-checkbox @change="savePrivacy('email', privacy.email)"
                                 v-model="privacy.email">公开展示我的电子邮件地址</el-checkbox>
                    <el-checkbox @change="savePrivacy('wx', privacy.wx)"
                                 v-model="privacy.wx">公开展示我的微信号</el-checkbox>
                    <el-checkbox @change="savePrivacy('qq', privacy.qq)"
                                 v-model="privacy.qq">公开展示我的QQ号</el-checkbox>
                    <el-checkbox @change="savePrivacy('gender', privacy.gender)"
                                 v-model="privacy.gender">公开展示我的性别</el-checkbox>
                </div>
            </card>
            <card style="margin: 20px 0" :icon="Setting"
                  title="修改密码" desc="修改密码请在这里进行操作，请务必牢记您的密码">
                <el-form :rules="rules" :model="form" ref="formRef" @validate="onValidate" label-width="100" style="margin: 20px">
                    <el-form-item label="当前密码" prop="password">
                        <el-input type="password" :prefix-icon="Lock" v-model="form.password"
                                  placeholder="当前密码" maxlength="20"/>
                    </el-form-item>
                    <el-form-item label="新密码" prop="new_password">
                        <el-input type="password" :prefix-icon="Lock" v-model="form.new_password"
                                  placeholder="新密码" maxlength="20"/>
                    </el-form-item>
                    <el-form-item label="重复新密码" prop="new_password_repeat">
                        <el-input type="password" :prefix-icon="Lock" v-model="form.new_password_repeat"
                                  placeholder="重新输入新密码" maxlength="20"/>
                    </el-form-item>
                    <div style="text-align: center">
                        <el-button @click="resetPassword" :icon="Switch" type="success">立即重置密码</el-button>
                    </div>
                </el-form>
            </card>
        </div>
    </div>
</template>

<script setup>

import Card from "@/components/Card.vue";
import {Setting, Switch, Lock} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {get, post, logout} from "@/net";
import {ElMessage} from "element-plus";
import router from "@/router"

/** 修改密码表单数据 */
const form = reactive({
    password: '',
    new_password: '',
    new_password_repeat: ''
})

/**
 * 重复密码校验器，确保两次输入的新密码一致
 *
 * @param rule     校验规则对象
 * @param value    当前输入值
 * @param callback 校验结果回调
 */
const validatePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== form.new_password) {
        callback(new Error("两次输入的密码不一致"))
    } else {
        callback()
    }
}

/** 密码修改表单校验规则 */
const rules = {
    password: [
        { required: true, message: '请输入原来的密码', trigger: 'blur' }
    ],
    new_password: [
        { required: true, message: '请输入新的密码', trigger: 'blur' },
        { min: 6, max: 16, message: '密码的长度必须在6-16个字符之间', trigger: ['blur'] }
    ],
    new_password_repeat: [
        { required: true, message: '请再次输入新的密码', trigger: 'blur' },
        { validator: validatePassword, trigger: ['blur', 'change'] },
    ]
}

/** 密码修改表单组件引用 */
const formRef = ref()
/** 表单整体校验是否通过 */
const valid = ref(false)

/**
 * 表单校验回调，更新整体校验状态
 *
 * @param prop     校验的字段名
 * @param isValid  校验是否通过
 */
const onValidate = (prop, isValid) => valid.value = isValid

/** 提交密码修改，校验通过后请求后端，成功后退出登录并跳转登录页 */
function resetPassword(){
    formRef.value.validate(valid => {
        if(valid) {
            post('/api/user/change-password', form, () => {
                ElMessage.success('修改密码成功，请重新登录！')
                logout(() => router.push('/'))
            })
        }
    })
}

/** 隐私设置加载状态 */
const saving = ref(true)

/** 各字段是否公开展示，true=公开，false=隐藏 */
const privacy = reactive({
    phone: false,
    wx: false,
    qq: false,
    email: false,
    gender: false
})

/** 页面初始化：拉取当前用户的隐私设置 */
get('/api/user/privacy', data => {
    privacy.phone = data.phone
    privacy.email = data.email
    privacy.wx = data.wx
    privacy.qq = data.qq
    privacy.gender = data.gender
    saving.value = false
})

/**
 * 保存单项隐私设置
 *
 * @param type   字段名（phone/email/wx/qq/gender）
 * @param status 是否公开
 */
function savePrivacy(type, status){
    saving.value = true
    post('/api/user/save-privacy', {
        type: type,
        status: status
    }, () => {
        ElMessage.success('隐私设置修改成功！')
        saving.value = false
    })
}
</script>

<style scoped>
.checkbox-list {
    margin: 10px 0 0 10px;
    display: flex;
    flex-direction: column;
}
</style>
