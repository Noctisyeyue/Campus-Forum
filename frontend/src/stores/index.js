import { defineStore } from "pinia";
import axios from "axios";

/**
 * 全局状态管理 Store（Pinia），存储当前登录用户信息、论坛分类、深色模式状态
 */
export const useStore = defineStore('general', {
    /** 响应式状态数据 */
    state: () => {
        return {
            /** 当前登录用户信息（登录后由 IndexView/AdminView 从后端获取并赋值） */
            user: {
                id: -1,
                username: '',
                email: '',
                role: '',
                avatar: null,
                registerTime: null
            },
            /** 论坛分类列表（由 Forum.vue 从后端获取并赋值） */
            forum: {
                types: []
            },
            /** 当前是否为深色模式（由 App.vue 中的 useDark() 赋值） */
            dark: false,
            /** 深色模式切换函数（由 App.vue 中的 useToggle(isDark) 赋值） */
            toggleDark: null
        }
    },
    /** 计算属性，依赖 state 自动更新 */
    getters: {
        /**
         * 当前登录用户的头像完整 URL
         * 有头像时拼接后端地址，无头像时返回 null（前端显示首字文字头像）
         */
        avatarUrl() {
            if(this.user.avatar)
                return `${axios.defaults.baseURL}/images${this.user.avatar}`
            return null
        },
        /** 当前登录用户是否为管理员 */
        isAdmin() {
            return this.user.role === 'admin'
        }
    },
    /** 方法 */
    actions: {
        /**
         * 根据分类 ID 查找分类对象
         *
         * @param id 分类 ID
         * @return 匹配的分类对象，未找到则返回 undefined
         */
        findTypeById(id) {
            for (let type of this.forum.types) {
                if(type.id === id)
                    return type
            }
        },
        /**
         * 根据头像路径生成完整 URL（用于列表中其他用户的头像）
         *
         * @param avatar 头像路径（如 /avatar/xxx.png）
         * @return 完整的头像 URL，无头像时返回 null
         */
        avatarUserUrl(avatar) {
            if(avatar)
                return `${axios.defaults.baseURL}/images${avatar}`
            return null
        },
        /**
         * 根据用户名生成文字头像内容：中文取首字，英文首字母转大写
         *
         * @param username 用户名
         * @return 文字头像内容
         */
        avatarText(username) {
            return (username?.[0] || '').toUpperCase()
        },
        /**
         * 根据用户名生成固定的头像背景色（同一用户每次颜色一致）
         *
         * @param username 用户名
         * @return 十六进制颜色值
         */
        avatarColor(username) {
            if(!username) return '#409EFF'                       // 没有用户名就用默认蓝色
            const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9B59B6', '#1ABC9C']// 7 种颜色备选
            let hash = 0                                           // 初始值 0
            for(let c of username) hash += c.charCodeAt(0)         // 遍历用户名的每个字符  把字符转成数字，累加
            return colors[hash % colors.length]                    // 取余数 → 得到颜色下标
        }
    }
})
