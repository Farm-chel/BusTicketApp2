package com.example.busticketapp2

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.graphics.Color
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.busticketapp2.Data.DatabaseHelper
import com.example.busticketapp2.models.User

class UserManagementActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listViewUsers: ListView
    private lateinit var btnBack: Button
    private lateinit var btnAddUser: Button

    private val usersList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)
        supportActionBar?.hide()

        dbHelper = DatabaseHelper(this)

        initViews()
        loadUsers()
        setupClickListeners()
    }

    private fun initViews() {
        listViewUsers = findViewById(R.id.listViewUsers)
        btnBack = findViewById(R.id.btnBack)
        btnAddUser = findViewById(R.id.btnAddUser)
    }

    private fun loadUsers() {
        usersList.clear()
        usersList.addAll(dbHelper.getAllUsers())

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            usersList.map { user ->
                "👤 ${user.fullName}\n" +
                        "📧 ${user.email} | 📱 ${user.phone}\n" +
                        "📧 Логин: ${user.username} | 🎯 ${user.role}"
            }
        )
        listViewUsers.adapter = adapter
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            finish()
        }

        btnAddUser.setOnClickListener {
            showAddUserDialog()
        }

        listViewUsers.setOnItemClickListener { parent, view, position, id ->
            val selectedUser = usersList[position]
            showUserActionsDialog(selectedUser)
        }
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val editUsername = dialogView.findViewById<EditText>(R.id.editUsername)
        val editPassword = dialogView.findViewById<EditText>(R.id.editPassword)
        val editFullName = dialogView.findViewById<EditText>(R.id.editFullName)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        // Только две роли доступны при создании пользователя
        val roles = arrayOf("Пассажир", "Кассир")

        // Используем тот же кастомный адаптер
        val roleAdapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item_black,
            roles
        ) {
            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as? TextView)?.setTextColor(Color.BLACK)
                return view
            }
        }

        roleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerRole.adapter = roleAdapter

        AlertDialog.Builder(this)
            .setTitle("➕ Добавить пользователя")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, which ->
                val username = editUsername.text.toString()
                val password = editPassword.text.toString()
                val fullName = editFullName.text.toString()
                val email = editEmail.text.toString().trim()
                val phone = editPhone.text.toString().trim()
                val role = spinnerRole.selectedItem.toString()

                if (username.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty() && email.isNotEmpty()) {
                    // Валидация email
                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Введите корректный email адрес", Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    if (dbHelper.isUsernameExists(username)) {
                        Toast.makeText(this, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show()
                    } else {
                        val newUser = User(
                            username = username,
                            password = password,
                            role = role,
                            fullName = fullName,
                            email = email,
                            phone = phone
                        )
                        val userId = dbHelper.addUser(newUser)
                        if (userId != -1L) {
                            Toast.makeText(this, "Пользователь успешно добавлен", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        } else {
                            Toast.makeText(this, "Ошибка при добавлении пользователя", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showUserActionsDialog(user: User) {
        val actions = mutableListOf<String>()

        // Всегда можно редактировать
        actions.add("Редактировать")

        // Всегда можно сменить пароль
        actions.add("Сменить пароль")

        // Удалять можно только не-администраторов
        if (user.role != "Администратор") {
            actions.add("Удалить")
        }

        AlertDialog.Builder(this)
            .setTitle("Действия с пользователем")
            .setItems(actions.toTypedArray()) { dialog, which ->
                when (actions[which]) {
                    "Редактировать" -> showEditUserDialog(user)
                    "Сменить пароль" -> showChangePasswordDialog(user)
                    "Удалить" -> showDeleteUserDialog(user)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showEditUserDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val editUsername = dialogView.findViewById<EditText>(R.id.editUsername)
        val editPassword = dialogView.findViewById<EditText>(R.id.editPassword)
        val editFullName = dialogView.findViewById<EditText>(R.id.editFullName)
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail)
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone)
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        // Заполняем поля текущими данными
        editUsername.setText(user.username)
        editPassword.setText(user.password)
        editFullName.setText(user.fullName)
        editEmail.setText(user.email)
        editPhone.setText(user.phone)

        // Определяем доступные роли для редактирования
        val roles: Array<String> = if (user.role == "Администратор") {
            arrayOf("Администратор", "Кассир", "Пассажир")
        } else {
            arrayOf("Пассажир", "Кассир", "Администратор")
        }

        // СОЗДАЕМ КАСТОМНЫЙ АДАПТЕР С ЧЕРНЫМ ТЕКСТОМ
        val roleAdapter = object : ArrayAdapter<String>(
            this,
            R.layout.spinner_item_black,
            roles
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                // Принудительно устанавливаем черный цвет
                (view as? TextView)?.setTextColor(Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                // Черный цвет для выпадающего списка
                (view as? TextView)?.setTextColor(Color.BLACK)
                return view
            }
        }

        spinnerRole.adapter = roleAdapter

        // Устанавливаем выбранную роль
        spinnerRole.post {
            val roleIndex = roles.indexOfFirst { it.equals(user.role, ignoreCase = true) }
            if (roleIndex >= 0) {
                spinnerRole.setSelection(roleIndex)
            }

            // Принудительно устанавливаем черный цвет для выбранного элемента
            val selectedView = spinnerRole.selectedView
            if (selectedView is TextView) {
                selectedView.setTextColor(Color.BLACK)
            }
        }

        // Скрываем поле пароля
        editPassword.visibility = View.GONE

        // Ищем и скрываем TextView "Пароль *"
        var passwordLabelFound = false
        for (i in 0 until (dialogView as ViewGroup).childCount) {
            val child = dialogView.getChildAt(i)
            if (child is TextView && child.text.toString().contains("Пароль")) {
                child.visibility = View.GONE
                passwordLabelFound = true
                break
            }
        }

        // Если не нашли через цикл, попробуем другой способ
        if (!passwordLabelFound) {
            // Создаем временный TextView для поиска
            val tempView = TextView(this)
            tempView.text = "Пароль"
            val passwordLabelId = tempView.id
            dialogView.findViewById<TextView>(passwordLabelId)?.visibility = View.GONE
        }

        // Делаем поле логина недоступным
        editUsername.isEnabled = false

        AlertDialog.Builder(this)
            .setTitle("✏️ Редактировать пользователя")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, which ->
                val email = editEmail.text.toString().trim()
                val phone = editPhone.text.toString().trim()
                val fullName = editFullName.text.toString().trim()
                val selectedRole = spinnerRole.selectedItem.toString()

                if (fullName.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Заполните обязательные поля (ФИО, Email)", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Валидация email
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Введите корректный email адрес", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                // Проверяем, можно ли менять роль администратора
                if (user.role == "Администратор" && selectedRole != "Администратор") {
                    val allUsers = dbHelper.getAllUsers()
                    val otherAdmins = allUsers.count {
                        it.role == "Администратор" && it.id != user.id
                    }

                    if (otherAdmins == 0) {
                        Toast.makeText(this,
                            "В системе должен быть хотя бы один администратор",
                            Toast.LENGTH_LONG).show()
                        return@setPositiveButton
                    }
                }

                val updatedUser = user.copy(
                    fullName = fullName,
                    email = email,
                    phone = phone,
                    role = selectedRole,
                    password = user.password // Сохраняем старый пароль
                )

                if (dbHelper.updateUser(updatedUser)) {
                    Toast.makeText(this, "Данные пользователя обновлены", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this, "Ошибка при обновлении данных", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showChangePasswordDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)
        val editNewPassword = dialogView.findViewById<EditText>(R.id.editNewPassword)
        val editConfirmPassword = dialogView.findViewById<EditText>(R.id.editConfirmPassword)

        AlertDialog.Builder(this)
            .setTitle("🔐 Сменить пароль")
            .setView(dialogView)
            .setPositiveButton("Сменить") { dialog, which ->
                val newPassword = editNewPassword.text.toString()
                val confirmPassword = editConfirmPassword.text.toString()

                if (newPassword == confirmPassword) {
                    if (newPassword.isNotEmpty()) {
                        val updatedUser = user.copy(password = newPassword)
                        if (dbHelper.updateUser(updatedUser)) {
                            Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Ошибка при смене пароля", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Пароль не может быть пустым", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteUserDialog(user: User) {
        AlertDialog.Builder(this)
            .setTitle("❌ Удаление пользователя")
            .setMessage("Вы уверены, что хотите удалить пользователя ${user.fullName}?")
            .setPositiveButton("Удалить") { dialog, which ->
                if (dbHelper.deleteUser(user.id)) {
                    Toast.makeText(this, "Пользователь удален", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this, "Ошибка при удалении пользователя", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}