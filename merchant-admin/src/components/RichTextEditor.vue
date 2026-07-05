<template>
  <div style="border: 1px solid #ccc; border-radius: 4px;">
    <div style="border-bottom: 1px solid #ccc;"></div>
    <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" style="border-bottom: 1px solid #ccc;" />
    <Editor :defaultConfig="editorConfig" v-model="valueHtml" :style="{ height: height + 'px', overflowY: 'hidden' }" @onCreated="handleCreated" />
  </div>
</template>

<script setup>
import { ref, shallowRef, onBeforeUnmount, watch, defineModel } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'

const props = defineProps({
  modelValue: { type: String, default: '' },
  height: { type: Number, default: 300 },
  placeholder: { type: String, default: '请输入内容...' }
})

const emit = defineEmits(['update:modelValue'])

const editorRef = shallowRef()
const valueHtml = ref(props.modelValue)

// Sync parent -> child
watch(() => props.modelValue, (val) => {
  if (valueHtml.value !== val) {
    valueHtml.value = val
  }
})

// Sync child -> parent
watch(valueHtml, (val) => {
  emit('update:modelValue', val)
})

const toolbarConfig = {
  excludeKeys: ['group-video']  // No video needed
}

const editorConfig = {
  placeholder: props.placeholder,
  MENU_CONF: {
    uploadImage: {
      // Images stored as base64 (simplifies setup without file server)
      base64LimitSize: 5 * 1024 * 1024 // 5MB
    }
  }
}

const handleCreated = (editor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor) {
    editor.destroy()
  }
})
</script>
