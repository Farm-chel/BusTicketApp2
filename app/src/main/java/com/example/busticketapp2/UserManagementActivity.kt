package com.example.busticketapp2

import android.os.Bundle
import android.widget.*
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
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail) // Изменено
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone) // Добавлено
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        // Только две роли доступны при создании пользователя
        val roles = arrayOf("Пассажир", "Кассир")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = roleAdapter

        AlertDialog.Builder(this)
            .setTitle("➕ Добавить пользователя")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, which ->
                val username = editUsername.text.toString()
                val password = editPassword.text.toString()
                val fullName = editFullName.text.toString()
                val email = editEmail.text.toString().trim() // Изменено
                val phone = editPhone.text.toString().trim() // Добавлено
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
        val actions = arrayOf("Редактировать", "Сменить пароль", "Удалить")

        AlertDialog.Builder(this)
            .setTitle("Действия с пользователем")
            .setItems(actions) { dialog, which ->
                when (which) {
                    0 -> showEditUserDialog(user)
                    1 -> showChangePasswordDialog(user)
                    2 -> showDeleteUserDialog(user)
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
        val editEmail = dialogView.findViewById<EditText>(R.id.editEmail) // Изменено
        val editPhone = dialogView.findViewById<EditText>(R.id.editPhone) // Добавлено
        val spinnerRole = dialogView.findViewById<Spinner>(R.id.spinnerRole)

        // Заполняем поля текущими данными
        editUsername.setText(user.username)
        editPassword.setText(user.password)
        editFullName.setText(user.fullName)
        editEmail.setText(user.email) // Изменено
        editPhone.setText(user.phone) // Добавлено

        val roles = arrayOf("Пассажир", "Кассир", "Администратор")
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = roleAdapter
        spinnerRole.setSelection(roles.indexOf(user.role))

        AlertDialog.Builder(this)
            .setTitle("✏️ Редактировать пользователя")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { dialog, which ->
                val email = editEmail.text.toString().trim()
                val phone = editPhone.text.toString().trim()

                // Валидация email
                if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Введите корректный email адрес", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                val updatedUser = user.copy(
                    username = editUsername.text.toString(),
                    password = editPassword.text.toString(),
                    fullName = editFullName.text.toString(),
                    email = email,
                    phone = phone,
                    role = spinnerRole.selectedItem.toString()
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