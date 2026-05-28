<template>
  <el-drawer :model-value="show"
             direction="btt"
             @open="initEditor"
             :close-on-click-modal="false"
             :size="650"
             @close="emit('close')">
    <template #header>
      <div>
        <div style="font-weight: bold">{{headerTitle}}</div>
        <div style="font-size: 13px">发表内容之前，请遵守相关法律法规，不要出现骂人等爆粗口的不文明行为。</div>
      </div>
    </template>
    <!-- 类型选择 + 标题输入 -->
    <div style="display: flex;gap: 10px">
      <div style="width: 150px">
        <el-select placeholder="选择主题类型..." value-key="id" v-model="editor.type" :disabled="!store.forum.types.length">
          <el-option v-for="item in selectableTypes" :value="item" :label="item.name">
            <div>
              <color-dot :color="item.color"/>
              <span style="margin-left: 10px">{{item.name}}</span>
            </div>
          </el-option>
        </el-select>
      </div>
      <div style="flex: 1">
        <el-input v-model="editor.title" placeholder="请输入帖子标题..." :prefix-icon="Document"
                  style="height: 100%" maxlength="30"/>
      </div>
    </div>
    <!-- 当前选中类型的描述 -->
    <div style="margin-top: 5px;font-size: 13px;color: grey">
      <color-dot :color="editor.type ? editor.type.color : '#dedede'"/>
      <span style="margin-left: 5px">{{editor.type ? editor.type.desc : '请在上方选择一个帖子类型'}}</span>
    </div>
    <!-- 富文本编辑器（Quill） -->
    <div style="margin-top: 10px;height: 440px;overflow: hidden;border-radius: 5px"
         v-loading="editor.uploading"
         element-loading-text="正在上传图片，请稍后...">
      <quill-editor v-model:content="editor.text" style="height: calc(100% - 45px)"
                    content-type="delta"
                    placeholder="今天想分享点什么呢？" :options="editorOption"/>
    </div>
    <!-- 底部字数统计 + 提交按钮 -->
    <div style="display: flex;justify-content: space-between;margin-top: 5px">
      <div style="color: grey;font-size: 13px">
        当前字数 {{contentLength}}（最大支持20000字）
      </div>
      <div>
        <el-button type="success" :icon="Check" @click="submitTopic" plain>{{submitButton}}</el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import {Check, Document} from "@element-plus/icons-vue";
import {computed, reactive} from "vue";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import { ImageExtend, QuillWatch } from "quill-image-super-solution-module";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import axios from "axios";
import {accessHeader, post} from "@/net";
import {ElMessage} from "element-plus";
import ColorDot from "@/components/ColorDot.vue";
import {useStore} from "@/stores/index";

/** Pinia 全局状态 */
const store = useStore()

/** 组件属性定义 */
const props = defineProps({
    /** 是否显示抽屉 */
    show: Boolean,
    /** 默认标题（编辑帖子时回填） */
    defaultTitle: {
        default: '',
        type: String
    },
    /** 默认内容（编辑帖子时回填，JSON格式的Delta） */
    defaultText: {
        default: '',
        type: String
    },
    /** 默认选中的分类ID（编辑帖子时回填） */
    defaultType: {
        default: null,
        type: Number
    },
    /** 提交按钮文字 */
    submitButton: {
        default: '立即发表主题',
        type: String
    },
    /** 抽屉标题 */
    headerTitle: {
        default: '发表新的帖子',
        type: String
    },
    /** 提交回调函数，默认为发帖逻辑 */
    submit: {
        default: (editor, success) => {
            post('/api/forum/create-topic', {
                type: editor.type.id,
                title: editor.title,
                content: editor.text
            }, () => {
                ElMessage.success("帖子发表成功，等待管理员审核！")
                success()
            })
        },
        type: Function
    }
})

/** 声明组件事件 */
const emit = defineEmits(['close', 'success'])

/** 编辑器状态 */
const editor = reactive({
    /** 当前选中的分类对象 */
    type: null,
    /** 帖子标题 */
    title: '',
    /** 帖子富文本内容（Quill Delta 格式） */
    text: new Delta(),
    /** 是否加载中 */
    loading: false,
    /** 是否正在上传图片 */
    uploading: false
})

/** 可选择的分类列表（排除"全部"和系统内置分类） */
const selectableTypes = computed(() => store.forum.types.filter(type => type.id > 0 && !type.systemKey))

/**
 * 抽屉打开时初始化编辑器（支持编辑模式回填）
 */
function initEditor() {
    editor.text = props.defaultText
        ? new Delta(JSON.parse(props.defaultText))
        : new Delta()
    editor.title = props.defaultTitle
    editor.type = findTypeById(props.defaultType)
}

/**
 * 将 Quill Delta 对象转换为纯文本（用于字数统计）
 *
 * @param delta Quill Delta 对象
 * @return 纯文本字符串（去除空白字符）
 */
function deltaToText(delta) {
    if(!delta.ops) return ""
    let str = ""
    for (let op of delta.ops)
        str += op.insert
    return str.replace(/\s/g, "")
}

/** 当前内容纯文本字数 */
const contentLength = computed(() => deltaToText(editor.text).length)

/**
 * 根据分类 ID 查找分类对象
 *
 * @param id 分类 ID
 * @return 匹配的分类对象，未找到返回 undefined
 */
function findTypeById(id){
    for (let type of store.forum.types) {
        if(type.id === id)
            return type
    }
}

/**
 * 提交帖子，校验字数、标题、分类后调用 submit 回调
 */
function submitTopic() {
    const text = deltaToText(editor.text)
    if(text.length > 20000) {
        ElMessage.warning('字数超出限制，无法发布主题！')
        return
    }
    if(!editor.title) {
        ElMessage.warning('请填写标题！')
        return
    }
    if(!editor.type) {
        ElMessage.warning('请选择一个合适的帖子类型！')
        return
    }
    props.submit(editor, () => emit('success'))
}

/** 注册 Quill 图片上传扩展模块 */
Quill.register('modules/ImageExtend', ImageExtend)

/** Quill 编辑器配置（工具栏 + 图片上传） */
const editorOption = {
    modules: {
        toolbar: {
            /** 工具栏按钮配置 */
            container: [
                "bold", // 加粗
                "italic", 
                "underline",
                "strike",
                "clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                { header: [1, 2, 3, 4, 5, 6, false] },
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                { indent: '-1' }, { indent: '+1' }
            ],
            /** 图片按钮点击时触发 ImageExtend 的图片选择 */
            handlers: {
                'image': function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        /** 图片上传扩展配置 */
        ImageExtend: {
            action:  axios.defaults.baseURL + '/api/image/cache',   // 上传接口地址
            name: 'file',                                            // 上传文件字段名
            size: 5,                                                 // 文件大小限制（MB）
            loading: true,                                           // 上传时显示loading
            accept: 'image/png, image/jpeg',                         // 允许的图片格式
            /** 上传成功后返回图片URL */
            response: (resp) => {
                if(resp.data) {
                    return axios.defaults.baseURL + '/images' + resp.data
                } else {
                    return null
                }
            },
            methods: 'POST',
            /** 上传请求添加 JWT 认证头 */
            headers: xhr => {
                xhr.setRequestHeader('Authorization', accessHeader().Authorization);
            },
            /** 上传开始回调 */
            start: () => editor.uploading = true,
            /** 上传成功回调 */
            success: () => {
                ElMessage.success('图片上传成功!')
                editor.uploading = false
            },
            /** 上传失败回调 */
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员!')
                editor.uploading = false
            }
        }
    }
}
</script>

<style scoped>
:deep(.el-drawer) {
    width: 800px;
    margin: auto;
    border-radius: 10px 10px 0 0;
}
:deep(.el-drawer__header) {
    margin: 0;
}
</style>
