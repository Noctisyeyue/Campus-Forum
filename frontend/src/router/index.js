import { createRouter, createWebHistory } from 'vue-router'
import { unauthorized } from "@/net";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'welcome',
            component: () => import('@/views/WelcomeView.vue'),
            children: [
                {
                    path: '',
                    name: 'welcome-login',
                    component: () => import('@/views/welcome/LoginPage.vue')
                }, {
                    path: 'register',
                    name: 'welcome-register',
                    component: () => import('@/views/welcome/RegisterPage.vue')
                }, {
                    path: 'forget',
                    name: 'welcome-forget',
                    component: () => import('@/views/welcome/ForgetPage.vue')
                }
            ]
        }, {
            path: '/index',
            name: 'index',
            component: () => import('@/views/IndexView.vue'),
            children: [
                {
                    path: '',
                    name: 'topics',
                    component: () => import('@/views/forum/Forum.vue'),
                    children: [
                        {
                            path: '',
                            name: 'topic-list',
                            component: () => import('@/views/forum/TopicList.vue')
                        },{
                            path: 'activity',
                            name: 'activity-list',
                            component: () => import('@/views/forum/ActivityList.vue')
                        },{
                            path: 'notice-topic',
                            name: 'notice-topic-list',
                            component: () => import('@/views/forum/NoticeTopicList.vue')
                        },{
                            path: 'topic-detail/:tid',
                            name: 'topic-detail',
                            component: () => import('@/views/forum/TopicDetail.vue')
                        }
                    ]
                }, {
                    path: 'user-setting',
                    name: 'user-setting',
                    component: () => import('@/views/settings/UserSetting.vue')
                }, {
                    path: 'privacy-setting',
                    name: 'privacy-setting',
                    component: () => import('@/views/settings/PrivacySetting.vue')
                }, {
                    path: 'my-topics',
                    name: 'my-topics',
                    component: () => import('@/views/forum/MyTopicList.vue')
                }
            ]
        }, {
            path: '/admin',
            name: 'admin',
            component: () => import('@/views/AdminView.vue'),
            children: [
                {
                    path: '',
                    name: 'admin-dashboard',
                    component: () => import('@/views/admin/DashboardPage.vue')
                }, {
                    path: 'topics',
                    name: 'admin-topics',
                    component: () => import('@/views/admin/TopicListPage.vue')
                }, {
                    path: 'topic-detail/:id',
                    name: 'admin-topic-detail',
                    component: () => import('@/views/admin/TopicDetailPage.vue')
                }, {
                    path: 'users',
                    name: 'admin-users',
                    component: () => import('@/views/admin/UserListPage.vue')
                }, {
                    path: 'user-detail/:id',
                    name: 'admin-user-detail',
                    component: () => import('@/views/admin/UserDetailPage.vue')
                }, {
                    path: 'comments',
                    name: 'admin-comments',
                    component: () => import('@/views/admin/CommentListPage.vue')
                }, {
                    path: 'types',
                    name: 'admin-types',
                    component: () => import('@/views/admin/TypeManagePage.vue')
                }, {
                    path: 'publish-activity',
                    name: 'admin-publish-activity',
                    component: () => import('@/views/admin/PublishActivityPage.vue')
                }, {
                    path: 'publish-notice-topic',
                    name: 'admin-publish-notice-topic',
                    component: () => import('@/views/admin/PublishNoticeTopicPage.vue')
                }, {
                    path: 'forum-notice',
                    name: 'admin-forum-notice',
                    component: () => import('@/views/admin/ForumNoticeSettingPage.vue')
                }, {
                    path: 'reports',
                    name: 'admin-reports',
                    component: () => import('@/views/admin/ReportListPage.vue')
                }
            ]
        }, {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: () => import('@/views/NotFound.vue')
        }
    ]
})

router.beforeEach((to, from, next) => {
    const isUnauthorized = unauthorized()
    if(to.name.startsWith('welcome') && !isUnauthorized) {
        next('/index')
    } else if(to.fullPath.startsWith('/admin') && isUnauthorized) {
        next('/')
    } else if(to.fullPath.startsWith('/index') && isUnauthorized) {
        next('/')
    } else {
        next()
    }
})

export default router
