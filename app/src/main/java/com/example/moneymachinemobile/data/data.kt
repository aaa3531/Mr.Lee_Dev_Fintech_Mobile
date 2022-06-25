package com.example.moneymachinemobile.data

import java.time.LocalDateTime
import kotlin.collections.ArrayList

var serverDataDone : Boolean = false
var yieldDataDone : Boolean = false
var orderModelDone : Boolean = false
var stockDone : Boolean = false
var signedDone : Boolean = false

var MMID : String = ""
var MMPW : String = ""
var ebestCert : String = ""
var ebestCertPW : String = ""
var AccountPW : String = ""
var AccountList = emptyList<String>()
var SelectedAccount : String = ""
var accountInfo : AccountInfo = AccountInfo(SelectedAccount, 0, 0, 0, 0, 0)
val stockList = mutableListOf<Stock>()

var user : UserModel = UserModel(0,"","","","","","",null,null,0,0,0,false,0,0,"")
var serverData : ServerData = ServerData(0,0,0)
var ratioModel : RatioModel = RatioModel(0,0)
var yieldList = ArrayList<YieldModel>()
var orderModelList = ArrayList<OrderModel>()
var buyOrderModelList = ArrayList<OrderModel>()
var sellOrderModelList = ArrayList<OrderModel>()

val userSellItemData = ArrayList<UserSellItemData>()
var allsignedModel = mutableListOf<체결>()
var signedModel = mutableListOf<체결>()
var unSignedModel = mutableListOf<체결>()
var signedItem = mutableListOf<TableItemData>()
var unSignedItem = mutableListOf<TableItemData>()

data class UserSellItemData(val 계좌: String, val 종목코드: String, val 종목명: String, val 잔고수량: String, val 매입금액: String, val 평가금액: String, val 평가손익: String, val 수익률: String, val 매도신호: String)
data class UserBuyItemData(val 계좌: String, val 종목코드: String, val 종목명: String, val 수량: String, val 추천가: String, val 매수신호: String)
data class TableItemData(val item1: String?, val item2: String, val item3: String, val item4: String)
data class 체결(val 원주문번호: String, val 종목코드: String, val 종목명: String?, val 주문구분: String, val 주문수량: String, val 주문가격: Long, val 현재가: Long, val 체결수량: Long, val 미체결잔량: Long, val 체결가격: Long, val 체결상태: String)

data class UserModel(
        var idx: Int,
        val id: String,
        var pw: String,
        val name: String,
        val email: String,
        val phone: String,
        val address: String,
        val signupdate: LocalDateTime?,
        val paiddate: LocalDateTime?,
        val expertidx: Int,
        var profit: Int,
        var totalestimatedassets: Int,
        val access: Boolean,
        val downpayment: Int,
        val companyidx: Int,
        val expertname: String,
)

data class ServerData(
        var profit : Int,
        var totalestimatedassets : Int,
        var memberCount : Int
)

data class AccountInfo(
        val 계좌번호 : String,
        var 추정순자산 : Long,
        var 매입금액 : Long,
        var 평가금액 : Long,
        var 평가손익 : Long,
        var 확정손익 : Long,
)

data class Stock(
        val 종목명: String,
        val 단축코드: String,
        val 확장코드: String,
        val ETF: String,
        val 상한가: Long,
        val 하한가: Long,
        val 전일가: Long,
        val 주문수량단위: String,
        val 기준가: Long,
        val 구분: String,
        )

data class 거래내역(
        val 거래일자: String,
        val 거래유형: String,
        val 종목명: String,
        val 외화수수료: Double,
        val 거래단가: Double,
        val 거래수량: Long,
        val 거래금액: Double,
        val 세금합계금액: Double
        )

data class Order(
        val orderdate : String,
        val expcode : String,
        val expname : String,
        val price : Int,
        val orderprice : Int,
        val cheprice : Long,
        val count : Int
)

data class OrderModel(
        val expertidx : Int,
        val expcode : String,
        val expname : String,
        val orderprice : Int,
        val price : Int,
        val orderdate : String,
        val ordertype : String,
        val status : Boolean,
        var ordercount : Int
)

data class YieldModel(
        val expertidx : Int,
        val expname : String,
        val orderprice : Int,
        val price : Int,
        val valuation : Int,
        val yield : Double,
)

data class RatioModel(
        val idx : Int,
        val ratio : Int,
)

data class CompanyModel(
        val idx : Int,
        val name : String,
        val id : String,
        val pw : String,
)
