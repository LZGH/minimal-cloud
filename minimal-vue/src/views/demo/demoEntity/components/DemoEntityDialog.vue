<template>
  <div>
    <!-- 添加或修改测试实体对话框 -->
    <el-dialog v-el-drag-dialog :title="dialog.title" :visible.sync="dialog.show" width="500px" @close="closeDialog">
      <el-form ref="form" :model="dialog.form" :rules="rules" label-width="80px">
        <el-form-item label="测试实体编码" prop="number">
          <el-input v-model="dialog.form.number" placeholder="请输入测试实体编码" />
        </el-form-item>
        <el-form-item label="测试实体名称" prop="name">
          <el-input v-model="dialog.form.name" placeholder="请输入测试实体名称" />
        </el-form-item>
        <el-form-item label="测试实体状态" prop="enabled">
          <el-radio-group v-model="dialog.form.enabled">
            <el-radio
              v-for="dict in dialog.dictValues['enabled']"
              :key="dict.key"
              :label="dict.key"
            >{{ dict.value }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="dialog.form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="closeDialog">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import elDragDialog from '@/directive/el-drag-dialog' // 可拖拽Dialog
import { insertDemoEntity, updateDemoEntity } from '@/api/demo/demoEntity' // 测试实体api
export default {
  name: 'DemoEntityDialog', // 组件名称
  components: {
  },
  directives: { elDragDialog }, // 注册可拖拽Dialog directive
  props: {
    dialog: Object // 从父组件接收的参数
  },
  data() {
    return {
      // 表单校验
      rules: {
        name: [
          { required: true, message: '测试实体名称不能为空', trigger: 'blur' }
        ],
        number: [
          { required: true, message: '测试实体编码不能为空', trigger: 'blur' }
        ]
      }
    }
  },
  // 初始化组件时执行
  created() {
  },
  methods: {
    /**
     * 关闭弹出框
     */
    closeDialog() {
      this.dialog.form = this.deepClone(this.dialog.defaultForm)
      this.resetForm('form')
      this.dialog.show = false
    },
    /**
     * 表单提交
     */
    submitForm() {
      this.$refs['form'].validate((valid) => {
        if (valid) {
          const model = this.dialog.form
          if (model.id) {
            updateDemoEntity(model.id, this.filterModel(model)).then(res => {
              this.$notify({
                title: '成功',
                message: '修改测试实体成功',
                type: 'success'
              })
              this.closeDialog()
              this.$emit('refresh')
            })
          } else {
            insertDemoEntity(this.filterModel(model)).then(res => {
              this.$notify({
                title: '成功',
                message: '新增测试实体成功',
                type: 'success'
              })
              this.closeDialog()
              this.$emit('refresh')
            })
          }
        } else {
          console.log('error submit!!')
          return false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>

</style>
