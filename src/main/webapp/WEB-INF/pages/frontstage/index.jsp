<%@ page language="java" contentType="text/html; charset=UTF-8"
pageEncoding="UTF-8"%>
<%@ include file="../_header.jsp"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>

<head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
    <title>收银端</title>
    <link rel="stylesheet" href="${ctx}/static/plugins/adminlte/css/AdminLTE.min.css">
    <link rel="stylesheet" href="${ctx}/static/plugins/adminlte/css/all-skins.min.css">
</head>

<body class="skin-green layout-top-nav">
    <div class="wrapper" id="app" v-cloak>
        <!-- header -->
        <header class="main-header">
            <nav class="navbar navbar-static-top">
                <div class="container-fluid">
                    <div class="navbar-header">
                        <a href="javascript:void(0);" class="navbar-brand">收银系统</a>
                        <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar-collapse">
                            <i class="fa fa-bars"></i>
                        </button>
                    </div>
                    <div class="collapse navbar-collapse" id="navbar-collapse">
                        <ul class="nav navbar-nav">
                            <li class="active"><a href="#cashRegister" @click="menuClick"><i class="fa fa-cny"></i>&nbsp;收银</a></li>
                            <li><a href="#memberPage" @click="menuClick"><i class="fa fa-user-circle"></i>&nbsp;会员管理</a></li>
                            <li><a href="#trade/queryTradeDetailListPage" @click="menuClick"><i class="fa fa-file-text"></i>&nbsp;销售单据</a></li>
                            <li><a href="#refundPage" @click="menuClick"><i class="fa fa-rotate-right"></i>&nbsp;退款</a></li>
                            <li><a href="#goodsStockAddPage" @click="menuClick"><i class="fa fa-truck"></i>&nbsp;进货</a></li>
                            <li><a href="#goodsEditPage" @click="menuClick"><i class="fa fa-shopping-bag"></i>&nbsp;商品编辑</a></li>
                            <li><a href="#goodsLosePage" @click="menuClick"><i class="fa fa-chain-broken"></i>&nbsp;报损</a></li>
                            <li><a href="#stockCheckPage" @click="menuClick"><i class="fa fa-dot-circle-o"></i>&nbsp;盘点</a></li>
                        </ul>
                        <ul class="nav navbar-nav navbar-right">
                            <li><a href="javascript:void(0);" @click="exchangeJob"><i class="fa fa-sign-out"></i>&nbsp;交接班</a></li>
                            <li class="dropdown"><a href="#" class="dropdown-toggle" data-toggle="dropdown"><i class="fa fa-cogs"></i>&nbsp;系统设置<span class="caret"></span></a>
                                <ul class="dropdown-menu" role="menu">
                                    <li><a href="#settings" @click="menuClick"><i class="fa fa-cog"></i>&nbsp;系统信息</a></li>
                                    <!-- <li><a href="#tagPrintSetPage" @click="menuClick"><i class="fa fa-cog"></i>&nbsp;标签打印设置</a></li> -->
                                    <li><a href="#receiptPrintSetPage" @click="menuClick"><i class="fa fa-cog"></i>&nbsp;小票打印设置</a></li>
                                </ul>
                            </li>
                        </ul>
                    </div>
                </div>
            </nav>
        </header>
        <!-- ./header -->
        <div id="exchangeJobDiv" class="container-fluid" style="display: none;">
        	<div class="div-height-15"></div>
        	<p>交班地址：(TODO 门店地址)</p>
        	<div class="div-height-15"></div>
        	<div class="row exchangeJobRow">
        		<div class="col-xs-8">收银员</div>
        		<div class="col-xs-4">${ sessionScope.seller.name }(工号：${ sessionScope.seller.sellerNo })</div>
        	</div>
        	<div class="row exchangeJobRow">
        		<div class="col-xs-8">总销售额</div>
        		<div class="col-xs-4">￥6579.00</div>
        	</div>
        	<div class="row exchangeJobRow">
        		<div class="col-xs-8">总单据数</div>
        		<div class="col-xs-4">52</div>
        	</div>
        	<div class="row exchangeJobRow">
        		<div class="col-xs-8">会员充值</div>
        		<div class="col-xs-4">￥300.00</div>
        	</div>
        	<div class="row exchangeJobRow">
        		<div class="col-xs-8">促销统计</div>
        		<div class="col-xs-4">￥979.00</div>
        	</div>
        	<p class="pull-right" style="font-size: 1.2em;color: red;">{{loginTime}}&nbsp;~&nbsp;{{exchangeJobTime}}</p>
        </div>
        <div class="content-wrapper">
            <!-- Main content -->
            <section class="content">
                <iframe scrolling="yes" frameborder="0" style="width: 100%; min-height: 200px; overflow: visible; background: #fff;" :src="iframeSrc"></iframe>
            </section>
            <!-- /.content -->
        </div>
        <!-- /.content-wrapper -->
        <footer class="main-footer"> Copyright &copy; 2018 All Rights Reserved </footer>
    </div>
    <script src="${ctx}/static/js/frontstage/index.js"></script>
</body>

</html>