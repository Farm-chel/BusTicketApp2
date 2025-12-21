package com.example.busticketapp2

import android.os.Bundle
import android.view.LayoutInflater
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
        loadUsersWithCustomAdapter()
        setupClickListeners()
    }

    private fun initViews() {
        listViewUsers = findViewById(R.id.listViewUsers)
        btnBack = findViewById(R.id.btnBack)
        btnAddUser = findViewById(R.id.btnAddUser)
    }

    private fun loadUsersWithCustomAdapter() {
        usersList.clear()
        usersList.addAll(dbHelper.getAllUsers())

        if (usersList.isEmpty()) {
            Toast.makeText(this, "В системе нет пользователей", Toast.LENGTH_SHORT).show()
        }

        val adapter = UserAdapter(this, usersList)
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
            if (position < usersList.size) {
                val selectedUser = usersList[position]
                showUserActionsDialog(selectedUser)
            }
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

        // Доступные роли при создании
        val roles = arrayOf("Пассажир", "Кассир")

        val roleAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_black,
            roles
        )
        roleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerRole.adapter = roleAdapter

        AlertDialog.Builder(this)
            .setTitle("➕ Добавить пользователя")
            .setView(dialogView)
            .setPositiveButton("Добавить") { dialog, which ->
                val username = editUsername.text.toString().trim()
                val password = editPassword.text.toString().trim()
                val fullName = editFullName.text.toString().trim()
                val email = editEmail.text.toString().trim()
                val phone = editPhone.text.toString().trim()
                val role = spinnerRole.selectedItem.toString()

                if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || email.isEmpty()) {
                    Toast.makeText(this, "Заполните все обязательные поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Валидация email
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Введите корректный email адрес", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                if (dbHelper.isUsernameExists(username)) {
                    Toast.makeText(this, "Пользователь с таким логином уже существует", Toast.LENGTH_SHORT).show()
                } else if (dbHelper.isEmailExists(email)) {
                    Toast.makeText(this, "Пользователь с таким email уже существует", Toast.LENGTH_SHORT).show()
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
                        loadUsersWithCustomAdapter()
                    } else {
                        Toast.makeText(this, "Ошибка при добавлении пользователя", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showUserActionsDialog(user: User) {
        val actions = mutableListOf<String>()

        actions.add("Редактировать")
        actions.add("Сменить пароль")

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

        // Доступные роли для редактирования
        val roles = when (user.role) {
            "Администратор" -> arrayOf("Администратор", "Кассир", "Пассажир")
            else -> arrayOf("Пассажир", "Кассир", "Администратор")
        }

        val roleAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item_black,
            roles
        )
        roleAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
        spinnerRole.adapter = roleAdapter

        // Устанавливаем выбранную роль
        val roleIndex = roles.indexOfFirst { it == user.role }
        if (roleIndex >= 0) {
            spinnerRole.setSelection(roleIndex)
        }

        // Скрываем поле пароля и его label
        editPassword.visibility = View.GONE
        val passwordLabel = editPassword.tag as? TextView
        passwordLabel?.visibility = View.GONE

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
                    Toast.makeText(this, "Заполните обязательные поля", Toast.LENGTH_SHORT).show()
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
                    password = user.password
                )

                if (dbHelper.updateUser(updatedUser)) {
                    Toast.makeText(this, "Данные пользователя обновлены", Toast.LENGTH_SHORT).show()
                    loadUsersWithCustomAdapter()
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

                if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(this, "Заполните оба поля", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword.length < 6) {
                    Toast.makeText(this, "Пароль должен быть не менее 6 символов", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPassword != confirmPassword) {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val updatedUser = user.copy(password = newPassword)
                if (dbHelper.updateUser(updatedUser)) {
                    Toast.makeText(this, "Пароль успешно изменен", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Ошибка при смене пароля", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteUserDialog(user: User) {
        AlertDialog.Builder(this)
            .setTitle("❌ Удаление пользователя")
            .setMessage("Вы уверены, что хотите удалить пользователя ${user.fullName}?\n\n" +
                    "📧 ${user.email}\n" +
                    "🎯 ${user.role}\n\n" +
                    "Это действие нельзя отменить!")
            .setPositiveButton("Удалить") { dialog, which ->
                if (dbHelper.deleteUser(user.id)) {
                    Toast.makeText(this, "Пользователь удален", Toast.LENGTH_SHORT).show()
                    loadUsersWithCustomAdapter()
                } else {
                    Toast.makeText(this, "Ошибка при удалении пользователя", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
}

// Кастомный адаптер для отображения пользователей
class UserAdapter(
    private val context: UserManagementActivity,
    private val users: List<User>
) : ArrayAdapter<User>(context, R.layout.item_user, users) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.item_user, parent, false)

        val user = users[position]

        val textUserName = view.findViewById<TextView>(R.id.textUserName)
        val textUserEmail = view.findViewById<TextView>(R.id.textUserEmail)
        val textUserPhone = view.findViewById<TextView>(R.id.textUserPhone)
        val textUserLogin = view.findViewById<TextView>(R.id.textUserLogin)
        val textUserRole = view.findViewById<TextView>(R.id.textUserRole)

        // Определяем цвет роли
        val roleColor = when (user.role) {
            "Администратор" -> "#F44336" // Красный
            "Кассир" -> "#FF9800"        // Оранжевый
            else -> "#4CAF50"             // Зеленый
        }

        textUserName.text = user.fullName
        textUserEmail.text = user.email
        textUserPhone.text = user.phone ?: "Не указан"
        textUserLogin.text = user.username
        textUserRole.text = user.role
        textUserRole.setTextColor(Color.parseColor(roleColor))

        return view
    }
}