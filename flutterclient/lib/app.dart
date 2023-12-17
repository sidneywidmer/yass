import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

import 'router.dart';

class YassApp extends StatefulWidget {
  const YassApp({super.key});

  @override
  State<YassApp> createState() => _YassAppState();
}

class _YassAppState extends State<YassApp> {
  late GoRouter router;

  @override
  void initState() {
    super.initState();
    router = appRouter();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Flutter Boilerplate',
      theme: ThemeData(primarySwatch: Colors.blue),
      routeInformationParser: router.routeInformationParser,
      routerDelegate: router.routerDelegate,
      routeInformationProvider: router.routeInformationProvider,
    );
  }
}
