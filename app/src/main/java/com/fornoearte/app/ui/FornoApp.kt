package com.fornoearte.app.ui

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.fornoearte.app.data.*
import com.fornoearte.app.integration.*
import com.fornoearte.app.util.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val Wine = Color(0xFF651A1A); private val Tomato = Color(0xFFC53B2C); private val Cream = Color(0xFFFFF8EE); private val Gold = Color(0xFFE3A72F)
private val currency = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

@Composable fun FornoApp(vm: OrderViewModel) {
    val nav = rememberNavController(); val state by vm.state.collectAsState(); val context = LocalContext.current
    val scheme = lightColorScheme(primary = Wine, secondary = Tomato, tertiary = Gold, background = Cream, surface = Color.White)
    MaterialTheme(colorScheme = scheme) {
        state.error?.let { AlertDialog(onDismissRequest=vm::clearError, confirmButton={TextButton(onClick=vm::clearError){Text("OK")}}, title={Text("Algo deu errado")}, text={Text(it)}) }
        Scaffold(bottomBar = { NavigationBar { listOf("home" to (Icons.Default.Home to "Início"), "orders" to (Icons.Default.ReceiptLong to "Pedidos"), "payment" to (Icons.Default.Payments to "Pagamentos")).forEach { (route,pair) -> NavigationBarItem(selected = nav.currentBackStackEntryAsState().value?.destination?.route == route, onClick={nav.navigate(route){launchSingleTop=true}}, icon={Icon(pair.first,null)}, label={Text(pair.second)}) } } }) { pad ->
            NavHost(nav, "home", Modifier.padding(pad)) {
                composable("home") { Dashboard(state, { nav.navigate("new") }, { nav.navigate("orders") }) }
                composable("orders") { OrdersScreen(state, vm::search, { nav.navigate("new") }, { nav.navigate("detail/${it.id}") }) }
                composable("new") { OrderForm(onBack={nav.popBackStack()}, onSave={ vm.save(it){ nav.popBackStack(); Toast.makeText(context,"Pedido salvo",Toast.LENGTH_SHORT).show() } }) }
                composable("detail/{id}") { back -> state.orders.find { it.id == back.arguments?.getString("id")?.toLongOrNull() }?.let { DetailScreen(it, vm, {nav.popBackStack()}, {nav.navigate("edit/${it.id}")}) } ?: Box(Modifier.fillMaxSize(), contentAlignment=Alignment.Center){Text("Pedido não encontrado")} }
                composable("edit/{id}") { back -> state.orders.find { it.id == back.arguments?.getString("id")?.toLongOrNull() }?.let { current -> OrderForm(current, {nav.popBackStack()}) { vm.save(it){nav.popBackStack()} } } }
                composable("payment") { PaymentsScreen(state.orders) }
            }
        }
    }
}

@Composable private fun Header(title: String, subtitle: String, back: (() -> Unit)? = null) { Surface(color=Wine, modifier=Modifier.fillMaxWidth()) { Row(Modifier.padding(20.dp), verticalAlignment=Alignment.CenterVertically) { if(back!=null) IconButton(back){Icon(Icons.Default.ArrowBack,null,tint=Color.White)}; Column { Text(title, color=Color.White, style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text(subtitle,color=Color.White.copy(.75f)) } } } }
@Composable private fun Dashboard(state: UiState, add:()->Unit, orders:()->Unit) { Column(Modifier.fillMaxSize().background(Cream)) { Header("Forno e Arte", "Gestão da pizzaria"); LazyColumn(contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) { item { Text("Olá! Confira o movimento de hoje.", style=MaterialTheme.typography.titleMedium) }; item { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Metric("Pedidos do dia",state.todayOrders.size.toString(),Icons.Default.LocalPizza,Modifier.weight(1f)); Metric("Faturamento",currency.format(state.todayOrders.sumOf{it.total}),Icons.Default.TrendingUp,Modifier.weight(1f)) } }; item { Button(add,Modifier.fillMaxWidth().height(54.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(8.dp));Text("Novo pedido")} }; item { Text("Pedidos recentes",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold) }; if(state.todayOrders.isEmpty()) item { Empty("Nenhum pedido registrado hoje") } else items(state.todayOrders.take(4)){ CompactOrder(it) }; item { OutlinedButton(orders,Modifier.fillMaxWidth()){Text("Ver todos os pedidos")} } } } }
@Composable private fun Metric(label:String,value:String,icon: androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier){Card(modifier){Column(Modifier.padding(16.dp)){Icon(icon,null,tint=Tomato);Spacer(Modifier.height(12.dp));Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(label,color=Color.Gray)}}}
@Composable private fun CompactOrder(o:OrderEntity){Card{Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){Surface(shape=RoundedCornerShape(12.dp),color=Cream){Icon(Icons.Default.LocalPizza,null,tint=Tomato,modifier=Modifier.padding(12.dp))};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text("#${o.id} • ${o.customerName}",fontWeight=FontWeight.Bold);Text(o.status.label,color=Wine)};Text(currency.format(o.total),fontWeight=FontWeight.Bold)}}}
@Composable private fun Empty(text:String){Box(Modifier.fillMaxWidth().padding(32.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.LocalPizza,null,tint=Color.LightGray,modifier=Modifier.size(48.dp));Text(text,color=Color.Gray)}}}

@Composable private fun OrdersScreen(state:UiState,search:(String)->Unit,add:()->Unit,open:(OrderEntity)->Unit){Column(Modifier.fillMaxSize().background(Cream)){Header("Pedidos","Histórico e acompanhamento"); OutlinedTextField(state.query,search,Modifier.fillMaxWidth().padding(16.dp),placeholder={Text("Buscar por cliente, telefone ou número")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true); if(state.orders.isEmpty()) Empty("Nenhum pedido encontrado") else LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){items(state.orders,key={it.id}){o->Card(onClick={open(o)}){Column(Modifier.padding(16.dp)){Row(Modifier.fillMaxWidth()){Text("Pedido #${o.id}",fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f));StatusChip(o.status)};Text(o.customerName,style=MaterialTheme.typography.titleMedium);Text(SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date(o.createdAt)),color=Color.Gray);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${o.quantity}× ${o.size}");Text(currency.format(o.total),fontWeight=FontWeight.Bold)}}}}}; FloatingActionButton(add,Modifier.align(Alignment.End).padding(20.dp),containerColor=Tomato){Icon(Icons.Default.Add,null,tint=Color.White)}}}
@Composable private fun StatusChip(status:OrderStatus){SuggestionChip(onClick={},label={Text(status.label)},colors=SuggestionChipDefaults.suggestionChipColors(containerColor=when(status){OrderStatus.DELIVERED->Color(0xFFDDF3E4);OrderStatus.OUT_FOR_DELIVERY->Color(0xFFFFE7C2);else->Color(0xFFF3DDDD)}))}

@Composable private fun OrderForm(existing:OrderEntity?=null,onBack:()->Unit,onSave:(OrderEntity)->Unit){
    var name by remember { mutableStateOf(existing?.customerName.orEmpty()) }
    var phone by remember { mutableStateOf(existing?.phone.orEmpty()) }
    var address by remember { mutableStateOf(existing?.address.orEmpty()) }
    var items by remember { mutableStateOf(existing?.items ?: "Pizza") }
    var flavors by remember { mutableStateOf(existing?.flavors.orEmpty()) }
    var size by remember { mutableStateOf(existing?.size ?: "Grande") }
    var qty by remember { mutableStateOf((existing?.quantity ?: 1).toString()) }
    var extras by remember { mutableStateOf(existing?.extras.orEmpty()) }
    var notes by remember { mutableStateOf(existing?.notes.orEmpty()) }
    var price by remember { mutableStateOf(existing?.unitPrice?.toString().orEmpty()) }
    var fee by remember { mutableStateOf(existing?.deliveryFee?.toString() ?: "0") }
    var discount by remember { mutableStateOf(existing?.discount?.toString() ?: "0") }
    var payment by remember { mutableStateOf(existing?.paymentMethod ?: PaymentMethod.PIX) }
    var error by remember { mutableStateOf<String?>(null) }

    fun number(value: String): Double = value.replace(",", ".").toDoubleOrNull() ?: 0.0

    val total = (
        number(price) * (qty.toIntOrNull() ?: 0) + number(fee) - number(discount)
    ).coerceAtLeast(0.0)
    Column(Modifier.fillMaxSize().background(Cream)){Header(if(existing==null)"Novo pedido" else "Editar pedido","Preencha os dados abaixo",onBack);LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Section("Cliente")};item{Field(name,{name=it},"Nome *")};item{Field(phone,{phone=it},"Telefone *",KeyboardType.Phone)};item{Field(address,{address=it},"Endereço de entrega *")};item{Section("Itens do pedido")};item{Field(items,{items=it},"Itens *")};item{Field(flavors,{flavors=it},"Sabores *")};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Dropdown(size,listOf("Broto","Média","Grande","Família")){size=it}};Box(Modifier.weight(1f)){Field(qty,{qty=it},"Quantidade *",KeyboardType.Number)}}};item{Field(extras,{extras=it},"Adicionais")};item{Field(notes,{notes=it},"Observações")};item{Section("Valores e pagamento")};item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){Field(price,{price=it},"Valor unitário *",KeyboardType.Decimal)};Box(Modifier.weight(1f)){Field(fee,{fee=it},"Taxa entrega",KeyboardType.Decimal)}}};item{Field(discount,{discount=it},"Desconto",KeyboardType.Decimal)};item{Dropdown(payment.label,PaymentMethod.entries.map{it.label}){label->payment=PaymentMethod.entries.first{it.label==label}}};item{Card(colors=CardDefaults.cardColors(containerColor=Wine)){Row(Modifier.fillMaxWidth().padding(20.dp),horizontalArrangement=Arrangement.SpaceBetween){Text("Total",color=Color.White);Text(currency.format(total),color=Color.White,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleLarge)}}};error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}};item{Button(onClick={if(name.isBlank()||phone.filter(Char::isDigit).length<10||address.isBlank()||flavors.isBlank()||number(price)<=0||qty.toIntOrNull()==null){error="Preencha os campos obrigatórios e informe um telefone e valores válidos"}else onSave(OrderEntity(existing?.id?:0,name.trim(),phone.trim(),address.trim(),items.trim(),flavors.trim(),size,qty.toInt(),extras.trim(),notes.trim(),number(price),number(fee),number(discount),payment,existing?.status?:OrderStatus.RECEIVED,existing?.createdAt?:System.currentTimeMillis()))},modifier=Modifier.fillMaxWidth().height(54.dp)){Text("Salvar pedido")}};item{Spacer(Modifier.height(24.dp))}}}
}
@Composable private fun Section(s:String)=Text(s,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold,color=Wine)
@Composable private fun Field(v:String,change:(String)->Unit,label:String,type:KeyboardType=KeyboardType.Text){OutlinedTextField(v,change,Modifier.fillMaxWidth(),label={Text(label)},keyboardOptions=KeyboardOptions(keyboardType=type),maxLines=if(label=="Observações")3 else 1)}
@Composable private fun Dropdown(value:String,options:List<String>,pick:(String)->Unit){var open by remember{mutableStateOf(false)};Box{OutlinedButton({open=true},Modifier.fillMaxWidth()){Text(value,Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(open,{open=false}){options.forEach{DropdownMenuItem({Text(it)},{pick(it);open=false})}}}}

@Composable
private fun DetailScreen(
    order: OrderEntity,
    vm: OrderViewModel,
    back: () -> Unit,
    edit: () -> Unit,
) {
    val context = LocalContext.current
    var deleteConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(Cream)) {
        Header("Pedido #${order.id}", order.customerName, back)
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cliente", fontWeight = FontWeight.Bold, color = Wine)
                        Text(order.customerName)
                        Text(order.phone)
                        Text(order.address)
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Pedido", fontWeight = FontWeight.Bold, color = Wine)
                        Text("${order.quantity}× ${order.items} • ${order.size}")
                        Text("Sabores: ${order.flavors}")
                        if (order.extras.isNotBlank()) Text("Adicionais: ${order.extras}")
                        if (order.notes.isNotBlank()) Text("Obs.: ${order.notes}")
                        HorizontalDivider(Modifier.padding(vertical = 10.dp))
                        Text("Total: ${currency.format(order.total)}", fontWeight = FontWeight.Bold)
                        Text("Pagamento: ${order.paymentMethod.label}")
                    }
                }
            }
            item { Section("Atualizar status") }
            items(OrderStatus.entries) { status ->
                OutlinedButton(
                    onClick = {
                        vm.setStatus(order, status) {
                            openWhatsApp(context, order.phone, whatsappMessage(order, status))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = order.status != status,
                ) {
                    Icon(
                        if (order.status == status) Icons.Default.CheckCircle else Icons.Default.Chat,
                        contentDescription = null,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (order.status == status) "${status.label} (atual)"
                        else "${status.label} • avisar no WhatsApp",
                    )
                }
            }
            item {
                Button(
                    onClick = { shareReceiptPdf(context, order) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Compartilhar comprovante PDF")
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = edit, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Text(" Editar")
                    }
                    OutlinedButton(
                        onClick = { deleteConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Text(" Excluir")
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (deleteConfirm) {
        AlertDialog(
            onDismissRequest = { deleteConfirm = false },
            title = { Text("Excluir pedido?") },
            text = { Text("Esta ação não poderá ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(order)
                    deleteConfirm = false
                    back()
                }) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirm = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable private fun PaymentsScreen(orders:List<OrderEntity>){val scope=rememberCoroutineScope();val gateway=remember{SimulatedInfinityPayGateway()};var selected by remember{mutableStateOf<OrderEntity?>(null)};var method by remember{mutableStateOf(PaymentMethod.PIX)};var result by remember{mutableStateOf<String?>(null)};var loading by remember{mutableStateOf(false)};Column(Modifier.fillMaxSize().background(Cream)){Header("Pagamentos","InfinityPay • modo simulado");LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF0D0))){Row(Modifier.padding(16.dp)){Icon(Icons.Default.Info,null,tint=Wine);Spacer(Modifier.width(10.dp));Text("Ambiente seguro de demonstração. A integração real exige credenciais configuradas em um servidor, nunca no aplicativo.")}}};item{Section("Selecione um pedido")};if(orders.isEmpty())item{Empty("Cadastre um pedido para simular o pagamento")}else items(orders.take(10)){o->Card(onClick={selected=o;result=null},colors=CardDefaults.cardColors(containerColor=if(selected?.id==o.id)Color(0xFFFFE5DF)else Color.White)){Row(Modifier.fillMaxWidth().padding(16.dp)){Text("#${o.id} • ${o.customerName}",Modifier.weight(1f),fontWeight=FontWeight.Bold);Text(currency.format(o.total))}}};item{Section("Forma de cobrança")};item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf(PaymentMethod.PIX,PaymentMethod.CARD,PaymentMethod.PAYMENT_LINK).forEach{FilterChip(selected=method==it,onClick={method=it},label={Text(it.label)})}}};item{Button(onClick={selected?.let{o->loading=true;result=null;scope.launch{result=when(val r=gateway.createPayment(PaymentRequest(o.id,o.total,method))){is PaymentResult.Success->"Cobrança simulada criada! Referência: ${r.reference}${r.paymentUrl?.let{"\nLink: $it"}.orEmpty()}";is PaymentResult.Error->r.message};loading=false}}},enabled=selected!=null&&!loading,modifier=Modifier.fillMaxWidth().height(54.dp)){if(loading)CircularProgressIndicator(Modifier.size(22.dp),color=Color.White)else Text("Simular cobrança")}};result?.let{item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFDDF3E4))){Text(it,Modifier.padding(16.dp),color=Color(0xFF175C2D),fontWeight=FontWeight.Bold)}}}}}}
