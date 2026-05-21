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
         * 有头像时拼接后端地址，无头像时使用 Element Plus 默认头像
         */
        avatarUrl() {
            if(this.user.avatar)
                /**  http://localhost:8081/images/avatar/123.png   */
                return `${axios.defaults.baseURL}/images${this.user.avatar}`
            else
                return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
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
         * @return 完整的头像 URL
         */
        avatarUserUrl(avatar) {
            if(avatar)
                return `${axios.defaults.baseURL}/images${avatar}`
            else
                return 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png'
        }
    }
})
