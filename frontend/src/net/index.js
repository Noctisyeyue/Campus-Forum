import axios from "axios";
import {ElMessage} from "element-plus";
import router from "@/router";

/** localStorage / sessionStorage 中存储认证信息的 key */
const authItemName = "authorize"

/** 构造带 Bearer Token 的请求头 */
const accessHeader = () => {
    return {
        'Authorization': `Bearer ${takeAccessToken()}`
    }
}

/** 默认网络异常处理（HTTP 请求本身失败，如服务器无响应） */
const defaultError = (error) => {
    console.error(error)
    const status = error.response.status
    if (status === 403 && error.response.data?.message?.includes('禁用')) {
        ElMessage.error('账号已被禁用，请联系管理员')
        deleteAccessToken(true)
    } else if (status === 429) {
        ElMessage.error(error.response.data.message)
    } else {
        ElMessage.error('发生了一些错误，请联系管理员')
    }
}

/** 默认业务失败处理（后端返回非 200 状态码），控制台打印调试信息并弹红色提示 */
const defaultFailure = (message, status, url) => {
    console.warn(`请求地址: ${url}, 状态码: ${status}, 错误信息: ${message}`)
    ElMessage({ message, type: 'error', duration: 3000 })
}

/**
 * 从浏览器存储中取出 Token，已过期则清除并返回 null
 *
 * @returns {string|null} Token 字符串，未登录或已过期返回 null
 */
function takeAccessToken() {
    const str = localStorage.getItem(authItemName) || sessionStorage.getItem(authItemName);
    if(!str) return null
    const authObj = JSON.parse(str)
    if(authObj.expire <= new Date()) {
        deleteAccessToken()
        ElMessage.warning("登录状态已过期，请重新登录！")
        return null
    }
    return authObj.token
}

/**
 * 将 Token 和过期时间存入浏览器存储
 *
 * @param remember {boolean} true 存 localStorage（持久），false 存 sessionStorage（关浏览器即失效）
 * @param token {string} JWT Token 字符串
 * @param expire {string} Token 过期时间
 */
function storeAccessToken(remember, token, expire){
    const authObj = {
        token: token,
        expire: expire
    }
    const str = JSON.stringify(authObj)
    if(remember)
        localStorage.setItem(authItemName, str)
    else
        sessionStorage.setItem(authItemName, str)
}

/**
 * 清除浏览器中存储的 Token
 *
 * @param redirect {boolean} true 表示清除后跳转到登录页
 */
function deleteAccessToken(redirect = false) {
    localStorage.removeItem(authItemName)
    sessionStorage.removeItem(authItemName)
    if(redirect) {
        router.push({ name: 'welcome-login' })
    }
}

/**
 * 内部通用 POST 请求，统一处理后端响应状态码
 *
 * @param url {string} 请求地址
 * @param data {object} 请求体数据
 * @param headers {object} 请求头
 * @param success {function} 业务成功回调，参数为 data 字段内容
 * @param failure {function} 业务失败回调，参数为 (message, status, url)
 * @param error {function} 网络异常回调，默认 defaultError
 */
function internalPost(url, data, headers, success, failure, error = defaultError){
    axios.post(url, data, { headers: headers }).then(({data}) => {
        if(data.code === 200) {
            success(data.data)
        } else if(data.code === 401) {
            failure('登录状态已过期，请重新登录！')
            deleteAccessToken(true)
        } else {
            failure(data.message, data.code, url)
        }
    }).catch(err => error(err))
}

/**
 * 内部通用 GET 请求，统一处理后端响应状态码
 *
 * @param url {string} 请求地址
 * @param headers {object} 请求头
 * @param success {function} 业务成功回调
 * @param failure {function} 业务失败回调
 * @param error {function} 网络异常回调，默认 defaultError
 */
function internalGet(url, headers, success, failure, error = defaultError){
    axios.get(url, { headers: headers }).then(({data}) => {
        if(data.code === 200) {
            success(data.data)
        } else if(data.code === 401) {
            failure('登录状态已过期，请重新登录！')
            deleteAccessToken(true)
        } else {
            failure(data.message, data.code, url)
        }
    }).catch(err => error(err))
}

/**
 * 用户登录，成功后自动存储 Token 并弹提示
 *
 * @param username {string} 用户名
 * @param password {string} 密码
 * @param remember {boolean} 是否记住登录
 * @param success {function} 登录成功回调，参数为用户信息（id, username, role, token, expire）
 * @param failure {function} 登录失败回调，默认 defaultFailure
 */
function login(username, password, remember, success, failure = defaultFailure){
    internalPost('/api/auth/login', {
        username: username,
        password: password
    }, {
        'Content-Type': 'application/x-www-form-urlencoded'
    }, (data) => {
        storeAccessToken(remember, data.token, data.expire)
        ElMessage.success(`登录成功，欢迎 ${data.username} 来到我们的系统`)
        success(data)
    }, failure)
}

/**
 * 需要认证的 POST 请求（自动携带 Token）
 *
 * @param url {string} 请求地址
 * @param data {object} 请求体数据
 * @param success {function} 成功回调
 * @param failure {function} 失败回调，默认 defaultFailure
 */
function post(url, data, success, failure = defaultFailure) {
    internalPost(url, data, accessHeader() , success, failure)
}

/**
 * 退出登录，后端将 Token 加入黑名单，前端清除本地存储
 *
 * @param success {function} 退出成功回调
 * @param failure {function} 退出失败回调，默认 defaultFailure
 */
function logout(success, failure = defaultFailure){
    get('/api/auth/logout', () => {
        deleteAccessToken()
        ElMessage.success(`退出登录成功，欢迎您再次使用`)
        success()
    }, failure)
}

/**
 * 需要认证的 GET 请求（自动携带 Token）
 *
 * @param url {string} 请求地址
 * @param success {function} 成功回调
 * @param failure {function} 失败回调，默认 defaultFailure
 */
function get(url, success, failure = defaultFailure) {
    internalGet(url, accessHeader(), success, failure)
}

/**
 * 判断当前是否未登录
 *
 * @returns {boolean} true=未登录，false=已登录
 */
function unauthorized() {
    return !takeAccessToken()
}

export { post, get, login, logout, unauthorized, accessHeader }
