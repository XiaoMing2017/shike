import axios from 'axios'

const client = axios.create({
  baseURL: '/api/v1',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// Request interceptor for Auth Token & User ID
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('shike_token')
  const userId = localStorage.getItem('shike_user_id')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (userId) {
    config.headers['X-User-Id'] = userId
  }
  return config
}, (error) => {
  return Promise.reject(error)
})

// Response interceptor
client.interceptors.response.use((response) => {
  // If backend returns ResultDTO format: { code: 200, message: '', data: ... }
  if (response.data && response.data.code !== undefined) {
    if (response.data.code === 200 || response.data.code === 0) {
      return response.data.data !== undefined ? response.data.data : response.data
    } else {
      return Promise.reject(new Error(response.data.message || 'Request failed'))
    }
  }
  return response.data
}, (error) => {
  const msg = error.response?.data?.message || error.message
  return Promise.reject(new Error(msg))
})

export default client
