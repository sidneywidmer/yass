import 'dart:io';

import 'package:dio/dio.dart';

class BaseClient {
  BaseClient(this.baseUrl);

  final String baseUrl;

  final BaseOptions _baseOptions = BaseOptions(
    connectTimeout: const Duration(seconds: 5),
  );

  final Dio _dio = Dio();

  Dio get dio => _dio;

  Future<T?> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    bool showLoader = true,
  }) async {
    return _request<T>(
      _dio.get<T>(
        baseUrl + path,
        queryParameters: queryParameters,
      ),
      showLoader,
    );
  }

  Future<T?> post<T>(
    String path, {
    Map<String, dynamic>? data,
    bool showLoader = true,
  }) async {
    return _request<T>(
      _dio.post<T>(
        baseUrl + path,
        data: data,
      ),
      showLoader,
    );
  }

  Future<T?> _request<T>(Future<Response<T>> future, bool showLoader) async {
    if (showLoader) {
      _showLoader();
    }

    try {
      final response = await future;
      if (showLoader) {
        _hideLoader();
      }
      return response.data;
    } on DioException catch (e) {
      if (showLoader) {
        _hideLoader();
      }

      if (e.response?.statusCode == 500) {
        _showErrorOverlay();
      }

      rethrow;
    } on SocketException {
      if (showLoader) {
        _hideLoader();
      }

      _showErrorOverlay();

      rethrow;
    }
  }

  void _showLoader() {
    // Implement your global loading indicator logic here
  }

  void _hideLoader() {
    // Implement logic to hide the global loading indicator
  }

  void _showErrorOverlay() {
    // Implement logic to display a global error overlay
  }
}
